package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.error.processor.TaskException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LufthansaServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private CategorizeService categorizeService;
    @Mock
    private LufthansaRepository lufthansaRepository;
    @Mock
    private TaskMapper taskMapper;
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
    void shouldNotRegisterTaskWhenCategorizationWasNotSuccessful() {
        when(categorizeService.categorize(any(Task.class)))
            .thenReturn(Mono.error(new AiProcessingException("AI error")));

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .expectError(AiProcessingException.class)
            .verify();
    }

    @Test
    void shouldCompleteTaskWhenTaskWasRegisteredAndCategoryWasDetermined() {
        when(categorizeService.categorize(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                task.setCategory(TaskCategory.ARCHITECTURE_DESIGN);
                return Mono.just(task);
            });

        when(taskMapper.toResponse(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                return CreatedTaskResponse.builder().id(task.getId()).build();
            });

        when(taskMapper.toEntity(any(Task.class)))
            .thenReturn(new TaskEntity());

        when(lufthansaRepository.save(any()))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(taskMapper.toResponse(any(TaskEntity.class)))
            .thenReturn(CreatedTaskResponse.builder().build());

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(response.getId()))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenTaskWasNotRegistered() {

        sut.completeTask(UUID.randomUUID())
            .as(StepVerifier::create)
            .expectError(TaskException.class)
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenWhenTaskWasRegisteredAndCategoryIsNotDetermined() {
        when(categorizeService.categorize(any(Task.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(taskMapper.toResponse(any(Task.class)))
            .thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                return CreatedTaskResponse.builder().id(task.getId()).build();
            });

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .flatMap(response -> sut.completeTask(response.getId()))
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof TaskException &&
                throwable.getMessage().equals("Task category is not determined"))
            .verify();
    }
}