package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LufthansaServiceTest {

    @Mock
    private CategorizeService categorizeService;
    @Mock
    private TaskMapper taskMapper;
    @InjectMocks
    private LufthansaService sut;

    @Test
    void shouldRegisterTask() {
        when(categorizeService.categorize(anyString()))
            .thenReturn(Mono.just(TaskCategory.ARCHITECTURE_DESIGN));

        when(taskMapper.toEntity(Mockito.any(), Mockito.any()))
            .thenReturn(TaskEntity.builder().build());

        when(taskMapper.toResponse(Mockito.any()))
            .thenReturn(CreatedTaskResponse.builder().build());

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }
}