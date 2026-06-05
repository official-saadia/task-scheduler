package com.taskscheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool configuration for the task scheduler engine.
 *
 * <p>Configures a {@link ThreadPoolTaskExecutor} that manages concurrent
 * task execution. Pool size is fully configurable via {@code application.yml},
 * satisfying the non-functional requirement for adjustable concurrency
 * without code changes.</p>
 *
 * <p>Thread pool parameters:</p>
 * <ul>
 *   <li><b>Core pool size</b> — minimum threads always kept alive, even when idle</li>
 *   <li><b>Max pool size</b> — maximum threads allowed when queue is full</li>
 *   <li><b>Queue capacity</b> — number of tasks that can wait when all core threads are busy</li>
 *   <li><b>Thread name prefix</b> — prefix for thread names, useful for debugging and logging</li>
 * </ul>
 *
 * <p>Execution flow:</p>
 * <ol>
 *   <li>Tasks are first handled by core threads</li>
 *   <li>If all core threads are busy, tasks are queued</li>
 *   <li>If the queue is full, additional threads up to max pool size are created</li>
 *   <li>If max threads and queue are both full, the task is rejected</li>
 * </ol>
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Value("${app.thread-pool.core-size:5}")
    private int corePoolSize;

    @Value("${app.thread-pool.max-size:10}")
    private int maxPoolSize;

    @Value("${app.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Creates and configures the {@link ThreadPoolTaskExecutor} bean
     * used by the scheduler engine for concurrent task execution.
     *
     * <p>The executor is initialised eagerly via {@code initialize()} to
     * pre-start core threads and avoid cold-start latency on first task execution.</p>
     *
     * @return a fully configured and initialised {@link Executor} instance
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("TaskScheduler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
