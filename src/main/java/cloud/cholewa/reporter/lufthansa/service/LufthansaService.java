package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LufthansaService {

    private final CategorizeService categorizeService;
    private final LufthansaRepository lufthansaRepository;
    private final TaskMapper taskMapper;

    private Task processedTask;

    public Mono<CreatedTaskResponse> registerTask(final CreateTaskRequest request) {
        return initTask(request.getDescription())
            .flatMap(categorizeService::categorize)
            .map(taskMapper::toResponse)
            .doOnNext(task -> log.info("Registered task with id: {}", task.getId()));
    }

    public Mono<CreatedTaskResponse> completeTask(final UUID taskId) {
        return Mono.fromSupplier(() -> isTaskReadyToComplete(taskId))
            .map(isValid -> taskMapper.toEntity(processedTask))
            .flatMap(lufthansaRepository::save)
            .doOnNext(taskEntity -> log.info("Completed task with id: {}", taskEntity.getId()))
            .map(taskMapper::toResponse);
    }

    private Mono<Task> initTask(final String taskDescription) {
        return Mono.fromSupplier(() -> processedTask = Task.builder().id(UUID.randomUUID()).description(
                taskDescription).build())
            .doOnNext(task -> log.info("Initialized new task with id: {}", task.getId()));
    }

    private boolean isTaskReadyToComplete(final UUID taskId) {
        if (processedTask == null) {
            throw new TaskException("Task not registered yet");
        } else if (!processedTask.getId().equals(taskId)) {
            throw new TaskException("Invalid task id: " + taskId + " task can not be completed");
        } else if (processedTask.getCategory() == null) {
            throw new TaskException("Task category is not determined");
        }
        return true;
    }
}
