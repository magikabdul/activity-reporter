package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LufthansaServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private CategorizeService categorizeService;
    @InjectMocks
    private LufthansaService sut;

    @Test
    void shouldRegisterTask() {
        when(categorizeService.categorize(anyString()))
            .thenReturn(Mono.just(TaskCategory.ARCHITECTURE_DESIGN));

        sut.registerTask(CreateTaskRequest.builder().description("some description").build())
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }
}