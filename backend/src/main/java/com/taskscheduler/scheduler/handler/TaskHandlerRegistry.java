package com.taskscheduler.scheduler.handler;

import com.taskscheduler.enums.TaskType;
import com.taskscheduler.exception.InvalidTaskTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry that maps {@link TaskType} values to their corresponding {@link TaskHandler} implementations.
 *
 * <p>At application startup, Spring injects all {@link TaskHandler} beans into this registry.
 * The registry builds a lookup map of {@code TaskType → TaskHandler}, allowing the
 * {@link com.taskscheduler.scheduler.TaskExecutorEngine} to resolve the correct handler
 * for any task type at runtime without any conditional logic.</p>
 *
 * <p>This design fully supports the <b>Open/Closed Principle</b>:</p>
 * <ul>
 *   <li><b>Open for extension</b> — add a new task type by creating a new {@link TaskHandler}
 *       implementation annotated with {@code @Component}. It is automatically discovered
 *       and registered here.</li>
 *   <li><b>Closed for modification</b> — neither this registry nor the executor engine
 *       requires any changes when new task types are added.</li>
 * </ul>
 *
 * <p>Currently registered handlers:</p>
 * <ul>
 *   <li>{@link EmailNotificationTaskHandler} → {@link TaskType#EMAIL_NOTIFICATION}</li>
 * </ul>
 */
@Slf4j
@Component
public class TaskHandlerRegistry {

    private final Map<TaskType, TaskHandler> handlers;

    /**
     * Constructs the registry by collecting all {@link TaskHandler} beans
     * and mapping them by their supported {@link TaskType}.
     *
     * @param taskHandlers all {@link TaskHandler} implementations discovered by Spring
     */
    public TaskHandlerRegistry(List<TaskHandler> taskHandlers) {
        this.handlers = taskHandlers.stream()
                .collect(Collectors.toMap(TaskHandler::getTaskType, Function.identity()));

        log.info("TaskHandlerRegistry: Registered {} handler(s): {}",
                handlers.size(),
                handlers.keySet().stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Resolves the {@link TaskHandler} for the given {@link TaskType}.
     *
     * @param taskType the task type to look up
     * @return the corresponding {@link TaskHandler} implementation
     * @throws InvalidTaskTypeException if no handler is registered for the given task type
     */
    public TaskHandler getHandler(TaskType taskType) {
        TaskHandler handler = handlers.get(taskType);
        if (handler == null) {
            throw new InvalidTaskTypeException(
                    "No handler registered for task type: " + taskType.name() +
                    ". Registered types: " + handlers.keySet());
        }
        return handler;
    }
}
