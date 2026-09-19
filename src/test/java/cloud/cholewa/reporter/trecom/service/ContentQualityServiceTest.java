package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.error.AiUnavailableException;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentQualityServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ContentQualityService sut;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        sut = new ContentQualityService(chatClientBuilder);
    }

    @Test
    void should_process_full_request_with_a_single_ai_call() {
        UUID taskId = UUID.randomUUID();
        CreateTaskRequest request = request("trecom", "jan", "kowalski", "Poprawny opis zadnia");
        request.setHoursSpent(8);
        request.setNotes("Notatki do zadnia");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {
              "firstName": "Jan",
              "lastName": "Kowalski",
              "description": "Poprawny opis zadania",
              "notes": "Notatki do zadania",
              "reasoning": "Fixed typos"
            }
            """);

        sut.process(request, Task.builder().id(taskId).build())
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getId()).isEqualTo(taskId);
                assertThat(result.getCustomer()).isEqualTo("TRECOM");
                assertThat(result.getHoursSpent()).isEqualTo(8);
                assertThat(result.getSalesmanFirstName()).isEqualTo("Jan");
                assertThat(result.getSalesmanLastName()).isEqualTo("Kowalski");
                assertThat(result.getDescription()).isEqualTo("Poprawny opis zadania");
                assertThat(result.getNotes()).isEqualTo("Notatki do zadania");
            })
            .verifyComplete();

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        // once for the stubbing above, once for the real call - what matters is that it is not one per field
        verify(chatClient, times(2)).prompt(prompt.capture());
        assertThat(prompt.getValue().getContents())
            .contains("jan", "kowalski", "Poprawny opis zadnia", "Notatki do zadnia");
    }

    @Test
    void should_leave_notes_empty_when_no_notes_were_given_whatever_ai_answers() {
        CreateTaskRequest request = request("trecom", "Jan", "Kowalski", "Poprawny opis zadania");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {"firstName": "Jan", "lastName": "Kowalski", "description": "Poprawny opis zadania",
             "notes": "(brak notatek)", "reasoning": "OK"}
            """);

        sut.process(request, Task.builder().id(UUID.randomUUID()).build())
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.getNotes()).isNull())
            .verifyComplete();
    }

    @Test
    void should_keep_the_given_notes_when_ai_returns_none() {
        CreateTaskRequest request = request("trecom", "Jan", "Kowalski", "Poprawny opis zadania");
        request.setNotes("Notatki do zadania");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {"firstName": "Jan", "lastName": "Kowalski", "description": "Poprawny opis zadania",
             "notes": "", "reasoning": "OK"}
            """);

        sut.process(request, Task.builder().id(UUID.randomUUID()).build())
            .as(StepVerifier::create)
            .assertNext(result -> assertThat(result.getNotes()).isEqualTo("Notatki do zadania"))
            .verifyComplete();
    }

    @Test
    void should_throw_exception_without_calling_ai_when_customer_is_missing() {
        CreateTaskRequest request = request(null, "Jan", "Kowalski", "Description");

        sut.process(request, Task.builder().build())
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof AiProcessingException &&
                throwable.getMessage().contains("Customer not provided"))
            .verify();

        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void should_throw_exception_when_firstname_is_not_valid() {
        CreateTaskRequest request = request("trecom", "NotAName", "Kowalski", "Description");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {"firstName": "false", "lastName": "Kowalski", "description": "Description",
             "notes": "", "reasoning": "Not a name"}
            """);

        sut.process(request, Task.builder().build())
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof AiProcessingException &&
                throwable.getMessage().contains("Provided word is not a firstname: NotAName"))
            .verify();
    }

    @Test
    void should_throw_exception_when_lastname_is_not_valid() {
        CreateTaskRequest request = request("trecom", "Jan", "NotALastname", "Description");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {"firstName": "Jan", "lastName": false, "description": "Description",
             "notes": "", "reasoning": "Not a lastname"}
            """);

        sut.process(request, Task.builder().build())
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof AiProcessingException &&
                throwable.getMessage().contains("Provided word is not a lastname: NotALastname"))
            .verify();
    }

    @Test
    void should_report_ai_as_unavailable_when_the_call_fails() {
        CreateTaskRequest request = request("trecom", "Jan", "Kowalski", "Description");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenThrow(new RuntimeException("timeout"));

        sut.process(request, Task.builder().build())
            .as(StepVerifier::create)
            .verifyErrorSatisfies(throwable -> assertThat(throwable)
                .isInstanceOf(AiUnavailableException.class)
                .hasRootCauseMessage("timeout"));
    }

    @Test
    void should_report_ai_as_unavailable_when_the_answer_is_incomplete() {
        CreateTaskRequest request = request("trecom", "Jan", "Kowalski", "Description");

        when(chatClient.prompt(any(Prompt.class)).call().content()).thenReturn("""
            {"firstName": "Jan", "lastName": "Kowalski", "reasoning": "cut off"}
            """);

        sut.process(request, Task.builder().build())
            .as(StepVerifier::create)
            .verifyError(AiUnavailableException.class);
    }

    private static CreateTaskRequest request(
        final String customer,
        final String firstName,
        final String lastName,
        final String description
    ) {
        Salesman salesman = new Salesman();
        salesman.setFirstName(firstName);
        salesman.setLastName(lastName);

        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer(customer);
        request.setSalesman(salesman);
        request.setDescription(description);
        return request;
    }
}
