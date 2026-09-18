package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.config.TaskDateResolver;
import cloud.cholewa.reporter.error.TaskNotFoundException;
import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.trecom.mapper.TrecomMapper;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.model.Task;
import cloud.cholewa.reporter.trecom.model.TaskEntity;
import cloud.cholewa.reporter.trecom.model.UpdateTaskRequest;
import cloud.cholewa.reporter.trecom.repository.TrecomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrecomServiceTasksTest {

    @Mock
    private ContentQualityService contentQualityService;
    @Mock
    private TrecomRepository trecomRepository;
    // the real mapper: these tests are about what ends up in the entity / response
    @Spy
    private TrecomMapper trecomMapper = Mappers.getMapper(TrecomMapper.class);
    // 00:30 on 1 October in Warsaw, while the container clock (UTC) still shows 30 September
    @Spy
    private TaskDateResolver taskDateResolver = new TaskDateResolver(
        Clock.fixed(Instant.parse("2026-09-30T22:30:00Z"), ZoneId.of("Europe/Warsaw")));
    @InjectMocks
    private TrecomService sut;

    @Test
    void should_date_registered_task_in_configured_time_zone_and_persist_notes() {
        when(contentQualityService.process(any(), any())).thenAnswer(invocation -> {
            final Task task = invocation.getArgument(1);
            task.setCustomer("ORLEN");
            task.setDescription("Corrected description");
            task.setHoursSpent(3);
            task.setSalesmanFirstName("Jan");
            task.setSalesmanLastName("Kowalski");
            task.setNotes("Corrected notes");
            return Mono.just(task);
        });
        when(trecomRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.registerTask(new CreateTaskRequest())
            .as(StepVerifier::create)
            .assertNext(response -> {
                assertThat(response.getCreatedAt()).isEqualTo(LocalDate.of(2026, 10, 1));
                assertThat(response.getNotes()).isEqualTo("Corrected notes");
            })
            .verifyComplete();
        final Task registered = (Task) ReflectionTestUtils.getField(sut, "processedTask");

        sut.completeTask(registered.getId())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        final ArgumentCaptor<TaskEntity> saved = ArgumentCaptor.forClass(TaskEntity.class);
        verify(trecomRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(saved.getValue().getCustomerName()).isEqualTo("ORLEN");
        assertThat(saved.getValue().getNotes()).isEqualTo("Corrected notes");
    }

    @Test
    void should_reject_registration_with_future_date() {
        final CreateTaskRequest request = new CreateTaskRequest();
        request.setCreatedAt(LocalDate.of(2026, 10, 2));

        sut.registerTask(request)
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();

        verify(contentQualityService, never()).process(any(), any());
    }

    @Test
    void should_return_tasks_of_the_month() {
        when(trecomRepository.findAllByDateRange(LocalDate.of(2026, 9, 1))).thenReturn(Flux.just(entity(1L)));

        sut.getMonthlyTasks(2026, 9)
            .as(StepVerifier::create)
            .assertNext(tasks -> {
                assertThat(tasks).hasSize(1);
                assertThat(tasks.getFirst().getId()).isEqualTo(1L);
                assertThat(tasks.getFirst().getCustomer()).isEqualTo("ORLEN");
                assertThat(tasks.getFirst().getSalesman().getFirstName()).isEqualTo("Jan");
                assertThat(tasks.getFirst().getSalesman().getLastName()).isEqualTo("Kowalski");
                assertThat(tasks.getFirst().getNotes()).isEqualTo("original notes");
            })
            .verifyComplete();
    }

    @Test
    void should_update_existing_task_keeping_its_id_and_upper_casing_customer() {
        when(trecomRepository.findById(7L)).thenReturn(Mono.just(entity(7L)));
        when(trecomRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.updateTask(7L, updateRequest(LocalDate.of(2026, 9, 20)))
            .as(StepVerifier::create)
            .assertNext(task -> {
                assertThat(task.getId()).isEqualTo(7L);
                assertThat(task.getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 20));
                assertThat(task.getCustomer()).isEqualTo("PKO BP");
                assertThat(task.getHoursSpent()).isEqualTo(5);
                assertThat(task.getSalesman().getFirstName()).isEqualTo("Anna");
                assertThat(task.getSalesman().getLastName()).isEqualTo("Nowak");
                assertThat(task.getNotes()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void should_fail_update_when_task_does_not_exist() {
        when(trecomRepository.findById(7L)).thenReturn(Mono.empty());

        sut.updateTask(7L, updateRequest(LocalDate.of(2026, 9, 20)))
            .as(StepVerifier::create)
            .expectError(TaskNotFoundException.class)
            .verify();

        verify(trecomRepository, never()).save(any());
    }

    @Test
    void should_reject_update_with_future_date() {
        sut.updateTask(7L, updateRequest(LocalDate.of(2026, 10, 2)))
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();

        verify(trecomRepository, never()).findById(any(Long.class));
    }

    @Test
    void should_delete_existing_task() {
        final TaskEntity entity = entity(7L);
        when(trecomRepository.findById(7L)).thenReturn(Mono.just(entity));
        when(trecomRepository.delete(entity)).thenReturn(Mono.empty());

        sut.deleteTask(7L)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(trecomRepository).delete(entity);
    }

    @Test
    void should_fail_delete_when_task_does_not_exist() {
        when(trecomRepository.findById(7L)).thenReturn(Mono.empty());

        sut.deleteTask(7L)
            .as(StepVerifier::create)
            .expectError(TaskNotFoundException.class)
            .verify();

        verify(trecomRepository, never()).delete(any(TaskEntity.class));
    }

    private static TaskEntity entity(final Long id) {
        return TaskEntity.builder()
            .id(id)
            .createdAt(LocalDate.of(2026, 9, 10))
            .customerName("ORLEN")
            .description("original description")
            .hoursSpent(2)
            .salesmanFirstName("Jan")
            .salesmanLastName("Kowalski")
            .notes("original notes")
            .build();
    }

    private static UpdateTaskRequest updateRequest(final LocalDate createdAt) {
        final Salesman salesman = new Salesman();
        salesman.setFirstName("Anna");
        salesman.setLastName("Nowak");

        return UpdateTaskRequest.builder()
            .createdAt(createdAt)
            .customer("pko bp")
            .description("updated description")
            .hoursSpent(5)
            .salesman(salesman)
            .build();
    }
}
