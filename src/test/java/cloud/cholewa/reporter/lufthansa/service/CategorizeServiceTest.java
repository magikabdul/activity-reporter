package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorizeServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private CategorizeService sut;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        sut = new CategorizeService(chatClientBuilder);
    }

    @Test
    void categorize_shouldReturnCategory_whenAiReturnsValidCategory() {
        Task task = Task.builder().description("Naprawa błędu w logowaniu").build();
        String aiResponse = """
            {
              "category": "BUG_FIXING_AND_MAINTENANCE",
              "reasoning": "Opis dotyczy naprawy błędu"
            }
            """;

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(aiResponse);

        sut.categorize(task)
            .as(StepVerifier::create)
            .assertNext(t -> assertThat(t)
                .returns(TaskCategory.BUG_FIXING_AND_MAINTENANCE, Task::getCategory)
                .returns("Naprawa błędu w logowaniu", Task::getDescription))
            .verifyComplete();
    }

    @Test
    void categorize_shouldThrowException_whenAiReturnsUnknownCategory() {
        Task task = Task.builder().description("Picie kawy").build();
        String aiResponse = """
            {
              "category": "UNKNOWN",
              "reasoning": "Nie dotyczy pracy IT"
            }
            """;

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(aiResponse);

        sut.categorize(task)
            .as(StepVerifier::create)
            .expectError(AiProcessingException.class)
            .verify();
    }

    @Test
    void categorize_shouldThrowException_whenAiCallFails() {
        Task task = Task.builder().description("Dowolne zadanie").build();
        when(chatClient.prompt(any(Prompt.class)).call()).thenThrow(new RuntimeException("AI error"));

        sut.categorize(task)
            .as(StepVerifier::create)
            .expectError(RuntimeException.class)
            .verify();
    }
}