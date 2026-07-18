package com.taskscheduler.scheduler.handler;

import com.taskscheduler.entity.ReportGenerationConfiguration;
import com.taskscheduler.entity.ReportGenerationExecution;
import com.taskscheduler.entity.Task;
import com.taskscheduler.entity.TaskExecution;
import com.taskscheduler.enums.BackupStatus;
import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.ResourceNotFoundException;
import com.taskscheduler.exception.TaskExecutionException;
import com.taskscheduler.repository.ReportGenerationExecutionRepository;
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
 * Task handler responsible for triggering external report-generation processes.
 *
 * <p>Implements {@link TaskHandler} for the {@link TaskType#REPORT_GENERATION}
 * type. Just like {@link DatabaseBackupTaskHandler}, Task Scheduler's job here
 * is narrow: run the configured command, wait for it to finish (or time out),
 * and record the exit code and output.</p>
 *
 * <p>One extra check beyond the backup handler: after the command finishes,
 * this handler verifies that {@link ReportGenerationConfiguration#getOutputFilePath()}
 * actually exists on disk. A script that exits 0 but never wrote a report is
 * still treated as a failed execution. What the report actually contains, and
 * how it's produced (a DB query, a script hitting this app's own DLQ API, a
 * BI export, etc.) is entirely outside Task Scheduler's concern.</p>
 */
@Slf4j
@Component
public class ReportGenerationTaskHandler implements TaskHandler {

    /** Max characters of combined stdout/stderr persisted per execution. */
    private static final int MAX_OUTPUT_CHARS = 8000;

    /** Fallback timeout when a configuration does not specify one. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    /** Grace period for the output reader to finish once the process has exited. */
    private static final int OUTPUT_DRAIN_TIMEOUT_SECONDS = 10;

    private final ReportGenerationExecutionRepository reportGenerationExecutionRepository;

    public ReportGenerationTaskHandler(ReportGenerationExecutionRepository reportGenerationExecutionRepository) {
        this.reportGenerationExecutionRepository = reportGenerationExecutionRepository;
    }

    /**
     * Executes a report generation task by running its configured command
     * and verifying the expected output file was produced.
     *
     * @param task          the task entity containing the report configuration
     * @param taskExecution the current execution record to be updated with results
     * @throws TaskExecutionException    if the command exits non-zero, times out,
     *                                    or the expected output file is missing afterward
     * @throws ResourceNotFoundException if the task has no report configuration attached
     */
    @Override
    public void execute(Task task, TaskExecution taskExecution) {
        ReportGenerationConfiguration config = task.getReportConfiguration();
        if (config == null) {
            throw new ResourceNotFoundException(
                    "No report generation configuration found for task: " + task.getId());
        }

        log.info("ReportGenerationTaskHandler: Executing task [{}] '{}' | command: {}",
                task.getId(), task.getName(), config.getCommand());

        LocalDateTime startedAt = LocalDateTime.now();
        int attemptNo = taskExecution.getRetryCount() + 1;

        int timeoutSeconds = config.getTimeoutSeconds() != null
                ? config.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;

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
            output = "[Task Scheduler] Failed to launch report command: " + ex.getMessage();
            log.error("ReportGenerationTaskHandler: Failed to launch command for task [{}]: {}",
                    task.getId(), ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            output = "[Task Scheduler] Interrupted while waiting for report command to finish.";
        }

        boolean commandSucceeded = !timedOut && exitCode != null && exitCode == 0;
        boolean fileProduced = commandSucceeded && new File(config.getOutputFilePath()).isFile();

        if (commandSucceeded && !fileProduced) {
            output += "\n[Task Scheduler] Command exited 0 but expected output file was not found at: "
                    + config.getOutputFilePath();
        }

        LocalDateTime completedAt = LocalDateTime.now();
        boolean success = commandSucceeded && fileProduced;

        persistExecution(taskExecution, config.getCommand(), exitCode, config.getOutputFilePath(), fileProduced,
                output, success ? BackupStatus.SUCCESS : BackupStatus.FAILED, attemptNo, startedAt, completedAt);

        if (success) {
            log.info("ReportGenerationTaskHandler: Task [{}] completed successfully, report at {}",
                    task.getId(), config.getOutputFilePath());
        } else {
            String reason;
            if (timedOut) {
                reason = "Report command timed out after " + timeoutSeconds + "s and was terminated";
            } else if (exitCode == null) {
                reason = "Report command could not be started";
            } else if (!commandSucceeded) {
                reason = "Report command exited with code " + exitCode;
            } else {
                reason = "Report command succeeded but expected output file was not produced at "
                        + config.getOutputFilePath();
            }
            log.error("ReportGenerationTaskHandler: Task [{}] failed. {}", task.getId(), reason);
            throw new TaskExecutionException(reason + " for task: " + task.getId());
        }
    }

    /**
     * Returns the task type this handler supports.
     *
     * @return {@link TaskType#REPORT_GENERATION}
     */
    @Override
    public TaskType getTaskType() {
        return TaskType.REPORT_GENERATION;
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
            Thread thread = new Thread(runnable, "report-output-reader");
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
                                  String outputFilePath, boolean fileProduced, String output,
                                  BackupStatus status, int attemptNo,
                                  LocalDateTime startedAt, LocalDateTime completedAt) {
        ReportGenerationExecution execution = ReportGenerationExecution.builder()
                .taskExecution(taskExecution)
                .command(command)
                .exitCode(exitCode)
                .outputFilePath(outputFilePath)
                .fileProduced(fileProduced)
                .status(status)
                .output(output)
                .attemptNo(attemptNo)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
        reportGenerationExecutionRepository.save(execution);
    }
}