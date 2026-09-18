package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.config.TaskDateResolver;
import cloud.cholewa.reporter.error.TaskNotFoundException;
import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import cloud.cholewa.reporter.lufthansa.model.UpdateTaskRequest;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
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
class LufthansaServiceTasksTest {

    @Mock
    private CategorizeService categorizeService;
    @Mock
    private LufthansaRepository lufthansaRepository;
    // the real mapper: these tests are about what ends up in the entity / response
    @Spy
    private TaskMapper taskMapper = Mappers.getMapper(TaskMapper.class);
    // 00:30 on 1 October in Warsaw, while the container clock (UTC) still shows 30 September
    @Spy
    private TaskDateResolver taskDateResolver = new TaskDateResolver(
        Clock.fixed(Instant.parse("2026-09-30T22:30:00Z"), ZoneId.of("Europe/Warsaw")));
    @InjectMocks
    private LufthansaService sut;

    @Test
    void shouldDateRegisteredTaskInConfiguredTimeZoneWhenNoDateWasGiven() {
        when(categorizeService.categorize(any(Task.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .assertNext(response -> assertThat(response.getCreatedAt()).isEqualTo(LocalDate.of(2026, 10, 1)))
            .verifyComplete();
    }

    @Test
    void shouldKeepRequestedDateUntilTaskIsPersisted() {
        when(categorizeService.categorize(any(Task.class))).thenAnswer(invocation -> {
            final Task task = invocation.getArgument(0);
            task.setCategory(TaskCategory.DOCUMENTATION);
            return Mono.just(task);
        });
        when(lufthansaRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.registerTask(CreateTaskRequest.builder()
                .description("some description")
                .createdAt(LocalDate.of(2026, 9, 12))
                .build())
            .block();
        final Task registered = (Task) ReflectionTestUtils.getField(sut, "processedTask");

        sut.completeTask(registered.getId())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        final ArgumentCaptor<TaskEntity> saved = ArgumentCaptor.forClass(TaskEntity.class);
        verify(lufthansaRepository).save(saved.capture());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(saved.getValue().getId()).isNull();
    }

    @Test
    void shouldRejectRegistrationWithFutureDate() {
        sut.registerTask(CreateTaskRequest.builder()
                .description("some description")
                .createdAt(LocalDate.of(2026, 10, 2))
                .build())
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();
    }

    @Test
    void shouldReturnTasksOfTheMonth() {
        when(lufthansaRepository.findAllByDateRange(LocalDate.of(2026, 9, 1)))
            .thenReturn(Flux.just(entity(1L), entity(2L)));

        sut.getMonthlyTasks(2026, 9)
            .as(StepVerifier::create)
            .assertNext(tasks -> {
                assertThat(tasks).hasSize(2);
                assertThat(tasks.getFirst().getId()).isEqualTo(1L);
                assertThat(tasks.getFirst().getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 10));
                assertThat(tasks.getFirst().getCategory()).isEqualTo(TaskCategory.DOCUMENTATION);
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyListWhenMonthHasNoTasks() {
        when(lufthansaRepository.findAllByDateRange(any())).thenReturn(Flux.empty());

        sut.getMonthlyTasks(2026, 9)
            .as(StepVerifier::create)
            .assertNext(tasks -> assertThat(tasks).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldUpdateExistingTaskKeepingItsId() {
        when(lufthansaRepository.findById(7L)).thenReturn(Mono.just(entity(7L)));
        when(lufthansaRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        sut.updateTask(7L, updateRequest(TaskCategory.BUG_FIXING_AND_MAINTENANCE, LocalDate.of(2026, 9, 20)))
            .as(StepVerifier::create)
            .assertNext(task -> {
                assertThat(task.getId()).isEqualTo(7L);
                assertThat(task.getCategory()).isEqualTo(TaskCategory.BUG_FIXING_AND_MAINTENANCE);
                assertThat(task.getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 20));
                assertThat(task.getDescription()).isEqualTo("updated description");
            })
            .verifyComplete();
    }

    @Test
    void shouldFailUpdateWhenTaskDoesNotExist() {
        when(lufthansaRepository.findById(7L)).thenReturn(Mono.empty());

        sut.updateTask(7L, updateRequest(TaskCategory.DOCUMENTATION, LocalDate.of(2026, 9, 20)))
            .as(StepVerifier::create)
            .expectError(TaskNotFoundException.class)
            .verify();

        verify(lufthansaRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateToUnknownCategory() {
        sut.updateTask(7L, updateRequest(TaskCategory.UNKNOWN, LocalDate.of(2026, 9, 20)))
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();

        verify(lufthansaRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdateWithFutureDate() {
        sut.updateTask(7L, updateRequest(TaskCategory.DOCUMENTATION, LocalDate.of(2026, 10, 2)))
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();

        verify(lufthansaRepository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingTask() {
        final TaskEntity entity = entity(7L);
        when(lufthansaRepository.findById(7L)).thenReturn(Mono.just(entity));
        when(lufthansaRepository.delete(entity)).thenReturn(Mono.empty());

        sut.deleteTask(7L)
            .as(StepVerifier::create)
            .verifyComplete();

        verify(lufthansaRepository).delete(entity);
    }

    @Test
    void shouldFailDeleteWhenTaskDoesNotExist() {
        when(lufthansaRepository.findById(7L)).thenReturn(Mono.empty());

        sut.deleteTask(7L)
            .as(StepVerifier::create)
            .expectError(TaskNotFoundException.class)
            .verify();

        verify(lufthansaRepository, never()).delete(any(TaskEntity.class));
    }

    private static TaskEntity entity(final Long id) {
        return TaskEntity.builder()
            .id(id)
            .createdAt(LocalDate.of(2026, 9, 10))
            .category(TaskCategory.DOCUMENTATION)
            .description("original description")
            .build();
    }

    private static UpdateTaskRequest updateRequest(final TaskCategory category, final LocalDate createdAt) {
        return UpdateTaskRequest.builder()
            .createdAt(createdAt)
            .category(category)
            .description("updated description")
            .build();
    }
}
