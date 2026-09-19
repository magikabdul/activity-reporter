package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiUnavailableException;
import cloud.cholewa.reporter.config.TaskDateResolver;
import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.model.CompleteTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LufthansaServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private CategorizeService categorizeService;
    @Mock
    private LufthansaRepository lufthansaRepository;
    @Mock
    private TaskMapper taskMapper;
    // 00:30 on 1 October in Warsaw, while the container clock (UTC) still shows 30 September
    @Spy
    private TaskDateResolver taskDateResolver = new TaskDateResolver(
        Clock.fixed(Instant.parse("2026-09-30T22:30:00Z"), ZoneId.of("Europe/Warsaw")));
    @InjectMocks
    private LufthansaService sut;

    @Test
    void shouldRegisterTaskWhenCategorizationWasSuccessful() {
        when(categorizeService.categorize(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                task.setCategory(TaskCategory.ARCHITECTURE_DESIGN);
                return Mono.just(task);
            });

        when(taskMapper.toResponse(any(Task.class)))
            .thenReturn(CreatedTaskResponse.builder().id(UUID.randomUUID()).build());

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldNotRegisterTaskWhenAiCallFailed() {
        when(categorizeService.categorize(any(Task.class)))
            .thenReturn(Mono.error(new AiUnavailableException("AI error", new RuntimeException())));

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .expectError(AiUnavailableException.class)
            .verify();
    }

    @Test
    void shouldCompleteTaskWhenTaskWasRegisteredAndCategoryWasDetermined() {
        registerWith(TaskCategory.ARCHITECTURE_DESIGN);
        stubSaving();

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(response.getId(), null))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldCompleteUnclassifiedTaskWithTheCategoryPickedByHand() {
        registerWith(TaskCategory.UNKNOWN);
        stubSaving();

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(
                response.getId(),
                CompleteTaskRequest.builder().category(TaskCategory.DOCUMENTATION).build()))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).toEntity(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo(TaskCategory.DOCUMENTATION);
    }

    @Test
    void shouldThrowExceptionWhenTaskWasNotRegistered() {

        sut.completeTask(UUID.randomUUID(), null)
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenWhenTaskWasRegisteredAndCategoryIsNotDetermined() {
        registerWith(null);

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(response.getId(), null))
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof TaskException &&
                throwable.getMessage().equals("Task category is not determined"))
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenAiAnsweredUnknownAndNoCategoryWasPicked() {
        registerWith(TaskCategory.UNKNOWN);

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(response.getId(), null))
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof TaskException &&
                throwable.getMessage().equals("Task category is not determined"))
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenTheCategoryPickedByHandIsUnknown() {
        registerWith(TaskCategory.UNKNOWN);

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(
                response.getId(),
                CompleteTaskRequest.builder().category(TaskCategory.UNKNOWN).build()))
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof TaskException &&
                throwable.getMessage().equals("Task category can not be UNKNOWN"))
            .verify();
    }

    private void registerWith(final TaskCategory category) {
        when(categorizeService.categorize(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                task.setCategory(category);
                return Mono.just(task);
            });

        when(taskMapper.toResponse(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                return CreatedTaskResponse.builder().id(task.getId()).build();
            });
    }

    private void stubSaving() {
        when(taskMapper.toEntity(any(Task.class)))
            .thenReturn(new TaskEntity());

        when(lufthansaRepository.save(any()))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(taskMapper.toResponse(any(TaskEntity.class)))
            .thenReturn(CreatedTaskResponse.builder().build());
    }
}
