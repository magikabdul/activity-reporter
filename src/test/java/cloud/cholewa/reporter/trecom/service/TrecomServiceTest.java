package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.trecom.mapper.TrecomMapper;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.model.Task;
import cloud.cholewa.reporter.trecom.model.TaskEntity;
import cloud.cholewa.reporter.trecom.repository.TrecomRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrecomServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ContentQualityService contentQualityService;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private TrecomRepository trecomRepository;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private TrecomMapper trecomMapper;

    @InjectMocks
    private TrecomService sut;

    @Test
    void should_successfully_register_task() {
        when(contentQualityService.process(any(), any()))
            .thenReturn(Mono.just(Task.builder().build()));

        when(trecomMapper.toResponse(any(Task.class)))
            .thenReturn(CreatedTaskResponse.builder().build());

        sut.registerTask(createTaskRequest())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        final Task task = (Task) ReflectionTestUtils.getField(sut, "processedTask");
        Assertions.assertNotNull(task);
        Assertions.assertNotNull(task.getId());
        Assertions.assertInstanceOf(UUID.class, task.getId());

        verify(contentQualityService).process(any(), any());
        verify(trecomMapper).toResponse(any(Task.class));
        verifyNoMoreInteractions(contentQualityService, trecomMapper);
    }

    @Test
    void should_throw_exception_when_completing_not_registered_task() {

        sut.completeTask(UUID.randomUUID())
            .as(StepVerifier::create)
            .verifyErrorSatisfies(throwable -> {
                Assertions.assertInstanceOf(TaskException.class, throwable);
                Assertions.assertEquals("Task not registered yet", throwable.getMessage());
            });

        verifyNoInteractions(trecomRepository, trecomMapper);
    }

    @Test
    void should_throw_exception_when_completing_task_with_wrong_id() {
        ReflectionTestUtils.setField(sut, "processedTask", Task.builder().id(UUID.randomUUID()).build());

        sut.completeTask(UUID.randomUUID())
            .as(StepVerifier::create)
            .verifyErrorSatisfies(throwable -> {
                Assertions.assertInstanceOf(TaskException.class, throwable);
                Assertions.assertEquals("Task not found", throwable.getMessage());
            });

        verifyNoInteractions(trecomRepository, trecomMapper);
    }

    @Test
    void should_successfully_complete_task() {
        when(trecomMapper.toEntity(any())).thenReturn(TaskEntity.builder().build());

        when(trecomRepository.save(any())).thenReturn(Mono.just(TaskEntity.builder().build()));

        when(trecomMapper.toResponse(any(TaskEntity.class))).thenReturn(CreatedTaskResponse.builder().build());

        UUID taskId = UUID.randomUUID();
        ReflectionTestUtils.setField(sut, "processedTask", Task.builder().id(taskId).build());

        sut.completeTask(taskId)
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        final Task task = (Task) ReflectionTestUtils.getField(sut, "processedTask");
        Assertions.assertNull(task);

        verify(trecomMapper, times(1)).toEntity(any());
        verify(trecomRepository).save(any());
        verify(trecomMapper, times(1)).toResponse(any(TaskEntity.class));
        verifyNoMoreInteractions(trecomMapper, trecomRepository);
    }

    @Test
    void should_successfully_get_monthly_report() {
        when(trecomRepository.findAllByDateRange(LocalDate.of(2023, 1, 1)))
            .thenReturn(Flux.just(TaskEntity.builder().build()));

        when(trecomMapper.toReportResponse(any())).thenReturn(new ReportResponse());

        sut.getMonthlyReport(2023, 1)
            .as(StepVerifier::create)
            .assertNext(reportResponses ->
                org.assertj.core.api.Assertions.assertThat(reportResponses).hasSize(1))
            .verifyComplete();

        verify(trecomRepository).findAllByDateRange(LocalDate.of(2023, 1, 1));
        verify(trecomMapper, times(1)).toReportResponse(any());

        verifyNoMoreInteractions(trecomRepository, trecomMapper);
    }

    @Test
    void should_return_empty_list_when_no_tasks_for_monthly_report() {
        when(trecomRepository.findAllByDateRange(LocalDate.of(2023, 1, 1)))
            .thenReturn(Flux.empty());

        sut.getMonthlyReport(2023, 1)
            .as(StepVerifier::create)
            .assertNext(reportResponses -> org.assertj.core.api.Assertions.assertThat(reportResponses).isEmpty())
            .verifyComplete();

        verify(trecomRepository).findAllByDateRange(LocalDate.of(2023, 1, 1));
        verifyNoMoreInteractions(trecomRepository, trecomMapper);
    }

    private static CreateTaskRequest createTaskRequest() {
        Salesman salesman = new Salesman();
        salesman.setFirstName("Test");
        salesman.setLastName("User");

        CreateTaskRequest createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setCustomer("Test customer");
        createTaskRequest.setDescription("Test description");
        createTaskRequest.setHoursSpent(10);
        createTaskRequest.setSalesman(salesman);
        return createTaskRequest;
    }
}