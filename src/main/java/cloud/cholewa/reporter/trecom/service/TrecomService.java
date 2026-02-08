package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.trecom.mapper.TrecomMapper;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.Task;
import cloud.cholewa.reporter.trecom.repository.TrecomRepository;
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
public class TrecomService {

    private final ContentQualityService contentQualityService;
    private final TrecomRepository trecomRepository;
    private final TrecomMapper trecomMapper;

    private Task processedTask;

    public Mono<CreatedTaskResponse> registerTask(final CreateTaskRequest request) {
        return Mono.fromSupplier(() -> processedTask = Task.builder().id(UUID.randomUUID()).build())
            .doOnNext(task -> log.info("Processing task with id: {}", task.getId()))
            .flatMap(task -> contentQualityService.process(request, task))
            .map(trecomMapper::toResponse);

    }

    public Mono<CreatedTaskResponse> completeTask(final UUID taskId) {
        return Mono.fromCallable(() -> {
                if (processedTask == null) {
                    throw new TaskException("Task not registered yet");
                } else if (!processedTask.getId().equals(taskId)) {
                    throw new TaskException("Task not found");
                } else {
                    return processedTask;
                }
            })
            .doOnNext(task -> processedTask = null)
            .map(trecomMapper::toEntity)
            .flatMap(trecomRepository::save)
            .doOnNext(task -> log.info("Task with id {} completed successfully", task.getId()))
            .map(trecomMapper::toResponse);
    }

    public Mono<List<ReportResponse>> getMonthlyReport(final int year, final int month) {
        return trecomRepository.findAllByDateRange(LocalDate.of(year, month, 1))
            .collectList()
            .map(taskEntities -> taskEntities.stream()
                .map(trecomMapper::toReportResponse)
                .toList());
    }
}
