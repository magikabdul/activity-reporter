package cloud.cholewa.reporter.trecom.api;

import cloud.cholewa.reporter.config.ErrorHandlerConfig;
import cloud.cholewa.reporter.error.TaskNotFoundException;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.model.TaskResponse;
import cloud.cholewa.reporter.trecom.model.UpdateTaskRequest;
import cloud.cholewa.reporter.trecom.service.TrecomService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(TrecomController.class)
@Import(ErrorHandlerConfig.class)
class TrecomTasksControllerTest {

    private static final String VALID_UPDATE = """
        {
          "createdAt": "2026-09-12",
          "customer": "Orlen",
          "description": "updated description",
          "hoursSpent": 4,
          "salesman": {"firstName": "Anna", "lastName": "Nowak"},
          "notes": "some notes"
        }
        """;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private TrecomService trecomService;

    @Test
    void should_return_monthly_tasks_with_iso_dates() {
        final Salesman salesman = new Salesman();
        salesman.setFirstName("Anna");
        salesman.setLastName("Nowak");

        when(trecomService.getMonthlyTasks(2026, 9)).thenReturn(Mono.just(List.of(TaskResponse.builder()
            .id(7L)
            .createdAt(LocalDate.of(2026, 9, 10))
            .customer("ORLEN")
            .description("some description")
            .hoursSpent(4)
            .salesman(salesman)
            .build())));

        webTestClient.get().uri("/trecom/tasks?year=2026&month=9")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(7)
            .jsonPath("$[0].createdAt").isEqualTo("2026-09-10")
            .jsonPath("$[0].salesman.lastName").isEqualTo("Nowak")
            .jsonPath("$[0].hoursSpent").isEqualTo(4);
    }

    @Test
    void should_return_empty_array_instead_of_not_found_for_empty_month() {
        when(trecomService.getMonthlyTasks(anyInt(), anyInt())).thenReturn(Mono.just(List.of()));

        webTestClient.get().uri("/trecom/tasks?year=2026&month=9")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[]");
    }

    @Test
    void should_update_task() {
        when(trecomService.updateTask(eq(7L), any())).thenReturn(Mono.just(TaskResponse.builder().id(7L).build()));

        webTestClient.put().uri("/trecom/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(VALID_UPDATE)
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.id").isEqualTo(7);

        final ArgumentCaptor<UpdateTaskRequest> request = ArgumentCaptor.forClass(UpdateTaskRequest.class);
        verify(trecomService).updateTask(eq(7L), request.capture());
        assertThat(request.getValue().getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(request.getValue().getSalesman().getLastName()).isEqualTo("Nowak");
        assertThat(request.getValue().getNotes()).isEqualTo("some notes");
    }

    @Test
    void should_return_bad_request_when_update_is_invalid() {
        webTestClient.put().uri("/trecom/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(VALID_UPDATE.replace("\"Anna\"", "\"anna\"").replace("4,", "0,"))
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(trecomService);
    }

    @Test
    void should_return_not_found_with_body_when_updated_task_does_not_exist() {
        when(trecomService.updateTask(eq(7L), any())).thenReturn(Mono.error(new TaskNotFoundException(7L)));

        webTestClient.put().uri("/trecom/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(VALID_UPDATE)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.title").isEqualTo("Task not found")
            .jsonPath("$.description").isEqualTo("Task with id 7 does not exist");
    }

    @Test
    void should_delete_task() {
        when(trecomService.deleteTask(7L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/trecom/tasks/7")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
    }

    @Test
    void should_return_not_found_when_deleted_task_does_not_exist() {
        when(trecomService.deleteTask(7L)).thenReturn(Mono.error(new TaskNotFoundException(7L)));

        webTestClient.delete().uri("/trecom/tasks/7")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.title").isEqualTo("Task not found");
    }
}
