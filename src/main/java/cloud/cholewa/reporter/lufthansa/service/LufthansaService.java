package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
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

    private CreatedTaskResponse response;

    public Mono<CreatedTaskResponse> registerTask(final CreateTaskRequest request) {
        return initTask(request.getDescription())
            .flatMap(createdTaskResponse -> categorizeTask(request.getDescription(), createdTaskResponse))
            .doOnNext(task -> log.info("Registered task with id: {}", task.getId()));
    }

    public Mono<CreatedTaskResponse> completeTask(final UUID taskId) {
        return Mono.fromSupplier(() -> isTaskReadyToComplete(taskId))
            .map(isValid -> taskMapper.toEntity(response))
            .flatMap(lufthansaRepository::save)
            .doOnNext(task -> log.info("Completed task with id: {}", task.getId()))
            .map(taskMapper::toResponse);
    }

    private Mono<CreatedTaskResponse> initTask(final String taskDescription) {
        return Mono.fromSupplier(() -> response = CreatedTaskResponse.builder().id(UUID.randomUUID()).description(
                taskDescription).build())
            .doOnNext(taskResponse -> log.info("Initialized new task with id: {}", taskResponse.getId()));
    }

    private Mono<CreatedTaskResponse> categorizeTask(
        final String taskDescription,
        final CreatedTaskResponse task
    ) {
        return categorizeService.categorize(taskDescription)
            .map(taskCategory -> {
                task.setCategory(taskCategory);
                return task;
            });
    }

    private boolean isTaskReadyToComplete(final UUID taskId) {
        if (response == null) {
            throw new TaskException("Task not registered");
        } else if (!response.getId().equals(taskId)) {
            throw new TaskException("Invalid task id: " + taskId + " task can not be completed");
        }
        return true;
    }
}
