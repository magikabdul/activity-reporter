package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiProcessingException;
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
        String taskDescription = "Naprawa błędu w logowaniu";
        String aiResponse = """
            {
              "category": "BUG_FIXING_AND_MAINTENANCE",
              "reasoning": "Opis dotyczy naprawy błędu"
            }
            """;

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(aiResponse);

        sut.categorize(taskDescription)
            .as(StepVerifier::create)
            .expectNext(TaskCategory.BUG_FIXING_AND_MAINTENANCE)
            .verifyComplete();
    }

    @Test
    void categorize_shouldThrowException_whenAiReturnsUnknownCategory() {
        String taskDescription = "Picie kawy";
        String aiResponse = """
            {
              "category": "UNKNOWN",
              "reasoning": "Nie dotyczy pracy IT"
            }
            """;

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn(aiResponse);

        sut.categorize(taskDescription)
            .as(StepVerifier::create)
            .expectError(AiProcessingException.class)
            .verify();
    }

    @Test
    void categorize_shouldThrowException_whenAiCallFails() {
        String taskDescription = "Dowolne zadanie";
        when(chatClient.prompt(any(Prompt.class)).call()).thenThrow(new RuntimeException("AI error"));

        sut.categorize(taskDescription)
            .as(StepVerifier::create)
            .expectError(RuntimeException.class)
            .verify();
    }
}