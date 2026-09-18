package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.config.TaskDateResolver;
import cloud.cholewa.reporter.error.TaskNotFoundException;
import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskResponse;
import cloud.cholewa.reporter.lufthansa.model.UpdateTaskRequest;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LufthansaService {

    private final CategorizeService categorizeService;
    private final LufthansaRepository lufthansaRepository;
    private final TaskMapper taskMapper;
    private final TaskDateResolver taskDateResolver;

    private Task processedTask;

    public Mono<CreatedTaskResponse> registerTask(final CreateTaskRequest request) {
        return initTask(request)
            .flatMap(categorizeService::categorize)
            .map(taskMapper::toResponse)
            .doOnNext(task -> log.info("Registered task with id: {}", task.getId()));
    }

    public Mono<CreatedTaskResponse> completeTask(final UUID taskId) {
        return Mono.fromSupplier(() -> isTaskReadyToComplete(taskId))
            .map(isValid -> taskMapper.toEntity(processedTask))
            .flatMap(lufthansaRepository::save)
            .doOnNext(taskEntity -> log.info("Completed task with description: {}", taskEntity.getDescription()))
            .doOnNext(taskEntity -> processedTask = null)
            .map(taskMapper::toResponse);
    }

    public Mono<List<TaskResponse>> getMonthlyTasks(final int year, final int month) {
        return lufthansaRepository.findAllByDateRange(LocalDate.of(year, month, 1))
            .map(taskMapper::toTaskResponse)
            .collectList();
    }

    public Mono<TaskResponse> updateTask(final Long taskId, final UpdateTaskRequest request) {
        return Mono.fromRunnable(() -> validateUpdate(request))
            .then(Mono.defer(() -> lufthansaRepository.findById(taskId)))
            .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)))
            .doOnNext(taskEntity -> taskMapper.updateEntity(request, taskEntity))
            .flatMap(lufthansaRepository::save)
            .doOnNext(taskEntity -> log.info("Updated task with id: {}", taskEntity.getId()))
            .map(taskMapper::toTaskResponse);
    }

    public Mono<Void> deleteTask(final Long taskId) {
        return lufthansaRepository.findById(taskId)
            .switchIfEmpty(Mono.error(new TaskNotFoundException(taskId)))
            .flatMap(lufthansaRepository::delete)
            .doOnSuccess(unused -> log.info("Deleted task with id: {}", taskId));
    }

    private Mono<Task> initTask(final CreateTaskRequest request) {
        return Mono.fromSupplier(() -> processedTask = Task.builder()
                .id(UUID.randomUUID())
                .createdAt(taskDateResolver.resolve(request.getCreatedAt()))
                .description(request.getDescription())
                .build())
            .doOnNext(task -> log.info("Initialized new task with id: {}", task.getId()));
    }

    private void validateUpdate(final UpdateTaskRequest request) {
        taskDateResolver.resolve(request.getCreatedAt());

        if (request.getCategory() == TaskCategory.UNKNOWN) {
            throw new TaskException("Task category can not be UNKNOWN");
        }
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
