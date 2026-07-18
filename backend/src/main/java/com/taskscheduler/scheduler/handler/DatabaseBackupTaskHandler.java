package com.taskscheduler.scheduler.handler;

import com.taskscheduler.entity.BackupConfiguration;
import com.taskscheduler.entity.BackupExecution;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.BackupStatus;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.exception.TaskExecutionException;
import com.taskscheduler.repository.BackupExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Task handler responsible for triggering external database backup processes.
 *
 * <p>Implements {@link TaskHandler} for the {@link TaskType#DATABASE_BACKUP} type.
 * Task Scheduler's responsibility here is deliberately narrow: it runs the
 * shell command configured on the task's {@link BackupConfiguration}, waits
 * for it to finish (or times out), and records the exit code and captured
 * output.</p>
 *
 * <p>Whether the backup itself is actually valid — e.g. whether a database
 * dump succeeded, produced a non-corrupt file, etc. — is entirely up to the
 * external command being invoked. This handler only observes the process
 * exit code: {@code 0} is treated as success, any non-zero code (or a
 * timeout) is treated as failure and triggers the standard retry / DLQ
 * mechanism, exactly like a failed email send.</p>
 */
@Slf4j
@Component
public class DatabaseBackupTaskHandler implements TaskHandler {

    /** Max characters of combined stdout/stderr persisted per execution. */
    private static final int MAX_OUTPUT_CHARS = 8000;

    /** Max characters of command output appended to the DLQ failure reason. */
    private static final int MAX_FAILURE_REASON_CHARS = 1500;

    /** Fallback timeout when a configuration does not specify one. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    /** Grace period for the output reader to finish once the process has exited. */
    private static final int OUTPUT_DRAIN_TIMEOUT_SECONDS = 10;

    private final BackupExecutionRepository backupExecutionRepository;

    public DatabaseBackupTaskHandler(BackupExecutionRepository backupExecutionRepository) {
        this.backupExecutionRepository = backupExecutionRepository;
    }

    /**
     * Executes a database backup task by running its configured command.
     *
     * @param task          the task entity containing backup configuration
     * @param taskExecution the current execution record to be updated with results
     * @throws TaskExecutionException    if the command exits non-zero, times out, or cannot be launched
     * @throws ResourceNotFoundException if the task has no backup configuration attached
     */
    @Override
    public void execute(Task task, TaskExecution taskExecution) {
        BackupConfiguration config = task.getBackupConfiguration();
        if (config == null) {
            throw new ResourceNotFoundException(
                    "No backup configuration found for task: " + task.getId());
        }

        log.info("DatabaseBackupTaskHandler: Executing task [{}] '{}' | command: {}",
                task.getId(), task.getName(), config.getCommand());

        LocalDateTime startedAt = LocalDateTime.now();
        int attemptNo = taskExecution.getRetryCount() + 1;

        int timeoutSeconds = config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

        Integer exitCode = null;
        String output;
        boolean timedOut = false;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", config.getCommand())
                    .redirectErrorStream(true);

            if (config.getWorkingDirectory() != null && !config.getWorkingDirectory().isBlank()) {
                processBuilder.directory(new File(config.getWorkingDirectory()));
            }

            Process process = processBuilder.start();

            // Read on a separate thread so the wait below is actually a wait.
            // Reading inline blocked until the stream hit EOF — which only
            // happens when the process exits — so waitFor was only ever reached
            // after the command had already finished and the timeout could never
            // fire. A hung command hung this worker thread forever.
            Future<String> outputFuture = readOutputAsync(process);

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                timedOut = true;
                process.destroyForcibly();
                // Let the kill land before collecting output: the reader only
                // sees EOF once the process is gone and the pipe closes.
                process.waitFor();
            }

            String capturedOutput = awaitOutput(outputFuture);

            if (timedOut) {
                output = capturedOutput + "\n[Task Scheduler] Command timed out after " + timeoutSeconds + "s and was terminated.";
            } else {
                exitCode = process.exitValue();
                output = capturedOutput;
            }
        } catch (IOException ex) {
            output = "[Task Scheduler] Failed to launch backup command: " + ex.getMessage();
            log.error("DatabaseBackupTaskHandler: Failed to launch command for task [{}]: {}",
                    task.getId(), ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            output = "[Task Scheduler] Interrupted while waiting for backup command to finish.";
        }

        LocalDateTime completedAt = LocalDateTime.now();
        boolean success = !timedOut && exitCode != null && exitCode == 0;

        persistExecution(taskExecution, config.getCommand(), exitCode, output,
                success ? BackupStatus.SUCCESS : BackupStatus.FAILED, attemptNo, startedAt, completedAt);

        if (success) {
            log.info("DatabaseBackupTaskHandler: Task [{}] completed successfully (exit code 0)", task.getId());
        } else {
            String reason = describeFailure(timedOut, exitCode, timeoutSeconds);
            String message = buildFailureMessage(task, reason, output);
            log.error("DatabaseBackupTaskHandler: Task [{}] failed. {}", task.getId(), message);
            throw new TaskExecutionException(message);
        }
    }

    /**
     * Describes why the command failed, distinguishing the three cases the old
     * message conflated. A null exit code means the process never started —
     * reporting that as "exited with code null" was actively misleading, since
     * it implies the command ran.
     */
    private String describeFailure(boolean timedOut, Integer exitCode, int timeoutSeconds) {
        if (timedOut) {
            return "Backup command timed out after " + timeoutSeconds + "s and was terminated";
        }
        if (exitCode == null) {
            return "Backup command could not be started";
        }
        return "Backup command exited with code " + exitCode;
    }

    /**
     * Builds the message carried by {@link TaskExecutionException}.
     *
     * <p>This message is what {@code TaskExecutorEngine} persists as both
     * {@code task_execution_logs.message} and {@code task_dlq.failure_reason},
     * so it is the only diagnostic a user sees on the DLQ page. Appending the
     * command's own output is what turns an unactionable "exited with code null"
     * into the actual cause.</p>
     *
     * <p>The tail of the output is kept rather than the head: errors surface at
     * the end, while the start is usually progress logging.</p>
     */
    private String buildFailureMessage(Task task, String reason, String output) {
        StringBuilder message = new StringBuilder(reason)
                .append(" for task: ").append(task.getId());

        if (output != null && !output.isBlank()) {
            String trimmed = output.strip();
            if (trimmed.length() > MAX_FAILURE_REASON_CHARS) {
                trimmed = "...[earlier output truncated]\n"
                        + trimmed.substring(trimmed.length() - MAX_FAILURE_REASON_CHARS);
            }
            message.append(" | output: ").append(trimmed);
        }
        return message.toString();
    }

    /**
     * Returns the task type this handler supports.
     *
     * @return {@link TaskType#DATABASE_BACKUP}
     */
    @Override
    public TaskType getTaskType() {
        return TaskType.DATABASE_BACKUP;
    }

    /**
     * Starts draining the process's combined stdout/stderr on a daemon thread.
     *
     * <p>The read has to happen concurrently with the wait, not before it: a pipe
     * read only returns EOF once the process exits, so reading inline meant the
     * timeout was evaluated against a command that had already finished.</p>
     *
     * <p>Draining also has to happen at all. The OS pipe buffer is finite (~64KB
     * on Linux); a command that writes more than that blocks on its own write
     * until something reads, so an unread pipe would deadlock a chatty command
     * into a timeout regardless of how fast it really was.</p>
     */
    private Future<String> readOutputAsync(Process process) {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "backup-output-reader");
            thread.setDaemon(true);
            return thread;
        });
        try {
            return executor.submit(() -> readOutput(process));
        } finally {
            // Refuses further work and lets the thread die once the read
            // finishes. Does not disturb the read already submitted.
            executor.shutdown();
        }
    }

    /**
     * Collects the drained output, bounded by {@link #OUTPUT_DRAIN_TIMEOUT_SECONDS}
     * so that a reader which cannot finish degrades to a missing-output message
     * rather than reintroducing the very hang this fix removes.
     *
     * <p>Never throws: the output is a diagnostic, and losing it must not change
     * whether the execution is judged a success.</p>
     */
    private String awaitOutput(Future<String> outputFuture) {
        try {
            return outputFuture.get(OUTPUT_DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            outputFuture.cancel(true);
            return "[Task Scheduler] Timed out collecting command output.";
        } catch (ExecutionException ex) {
            return "[Task Scheduler] Failed to read command output: " + ex.getCause().getMessage();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            outputFuture.cancel(true);
            return "[Task Scheduler] Interrupted while collecting command output.";
        }
    }

    /**
     * Reads the combined stdout/stderr of a process, truncating to
     * {@link #MAX_OUTPUT_CHARS} to avoid persisting unbounded output.
     */
    private String readOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() < MAX_OUTPUT_CHARS) {
                    builder.append(line).append('\n');
                }
            }
        }
        if (builder.length() > MAX_OUTPUT_CHARS) {
            builder.setLength(MAX_OUTPUT_CHARS);
            builder.append("...[truncated]");
        }
        return builder.toString();
    }

    private void persistExecution(TaskExecution taskExecution, String command, Integer exitCode,
                                  String output, BackupStatus status, int attemptNo,
                                  LocalDateTime startedAt, LocalDateTime completedAt) {
        BackupExecution execution = BackupExecution.builder()
                .taskExecution(taskExecution)
                .command(command)
                .exitCode(exitCode)
                .status(status)
                .output(output)
                .attemptNo(attemptNo)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
        backupExecutionRepository.save(execution);
    }
}