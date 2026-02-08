package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void should_process_full_request_successfully() {
        UUID taskId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer("trecom");
        request.setHoursSpent(8);
        request.setDescription("Poprawny opis zadania");
        Salesman salesman = new Salesman();
        salesman.setFirstName("Jan");
        salesman.setLastName("Kowalski");
        request.setSalesman(salesman);
        request.setNotes("Notatki do zadania");

        Task task = Task.builder().id(taskId).build();

        when(chatClient.prompt(any(Prompt.class)).stream().content())
            .thenReturn(Flux.just("{\"message\": \"Jan\", \"reasoning\": \"OK\"}")) // firstname
            .thenReturn(Flux.just("{\"message\": \"Kowalski\", \"reasoning\": \"OK\"}")) // lastname
            .thenReturn(Flux.just("{\"message\": \"Poprawny opis zadania\", \"reasoning\": \"OK\"}")) // description
            .thenReturn(Flux.just("{\"message\": \"Notatki do zadania\", \"reasoning\": \"OK\"}")); // notes

        sut.process(request, task)
            .as(StepVerifier::create)
            .assertNext(result -> {
                assertThat(result.getId()).isEqualTo(taskId);
                assertThat(result.getCustomer()).isEqualTo("TRECOM");
                assertThat(result.getHoursSpent()).isEqualTo(8);
                assertThat(result.getSalesmanFirstName()).isEqualTo("Jan");
                assertThat(result.getSalesmanLastName()).isEqualTo("Kowalski");
                assertThat(result.getDescription()).isEqualTo("Poprawny opis zadania");
            })
            .verifyComplete();
    }

    @Test
    void should_throw_exception_when_customer_is_missing() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer(null);
        Salesman salesman = new Salesman();
        salesman.setFirstName("Jan");
        salesman.setLastName("Kowalski");
        request.setSalesman(salesman);
        request.setDescription("Description");
        Task task = Task.builder().build();

        sut.process(request, task)
            .as(StepVerifier::create)
            .expectError(AiProcessingException.class)
            .verify();
    }

    @Test
    void should_throw_exception_when_firstname_is_not_valid() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer("trecom");
        Salesman salesman = new Salesman();
        salesman.setFirstName("NotAName");
        salesman.setLastName("Kowalski");
        request.setSalesman(salesman);
        request.setDescription("Description");
        Task task = Task.builder().build();

        when(chatClient.prompt(any(Prompt.class)).stream().content())
            .thenReturn(Flux.just("{\"message\": \"false\", \"reasoning\": \"Not a name\"}")) // firstname
            .thenReturn(Flux.just("{\"message\": \"Kowalski\", \"reasoning\": \"OK\"}")) // lastname
            .thenReturn(Flux.just("{\"message\": \"Description\", \"reasoning\": \"OK\"}")); // description

        sut.process(request, task)
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof AiProcessingException &&
                throwable.getMessage().contains("Provided word is not a firstname: NotAName"))
            .verify();
    }

    @Test
    void should_throw_exception_when_lastname_is_not_valid() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer("trecom");
        Salesman salesman = new Salesman();
        salesman.setFirstName("Jan");
        salesman.setLastName("NotALastname");
        request.setSalesman(salesman);
        request.setDescription("Description");
        Task task = Task.builder().build();

        when(chatClient.prompt(any(Prompt.class)).stream().content())
            .thenReturn(Flux.just("{\"message\": \"Jan\", \"reasoning\": \"OK\"}")) // firstname
            .thenReturn(Flux.just("{\"message\": \"false\", \"reasoning\": \"Not a lastname\"}")) // lastname
            .thenReturn(Flux.just("{\"message\": \"Description\", \"reasoning\": \"OK\"}")); // description

        sut.process(request, task)
            .as(StepVerifier::create)
            .expectErrorMatches(throwable -> throwable instanceof AiProcessingException &&
                throwable.getMessage().contains("Provided word is not a lastname: NotALastname"))
            .verify();
    }
}
