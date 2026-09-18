package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.config.ErrorHandlerConfig;
import cloud.cholewa.reporter.error.TaskNotFoundException;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskResponse;
import cloud.cholewa.reporter.lufthansa.model.UpdateTaskRequest;
import cloud.cholewa.reporter.lufthansa.service.LufthansaReportService;
import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
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

@WebFluxTest(LufthansaController.class)
@Import(ErrorHandlerConfig.class)
class LufthansaTasksControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private LufthansaService lufthansaService;
    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private LufthansaReportService lufthansaReportService;

    @Test
    void shouldReturnMonthlyTasksWithIsoDates() {
        when(lufthansaService.getMonthlyTasks(2026, 9)).thenReturn(Mono.just(List.of(TaskResponse.builder()
            .id(7L)
            .createdAt(LocalDate.of(2026, 9, 10))
            .category(TaskCategory.DOCUMENTATION)
            .description("some description")
            .build())));

        webTestClient.get().uri("/lufthansa/tasks?year=2026&month=9")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(7)
            .jsonPath("$[0].createdAt").isEqualTo("2026-09-10")
            .jsonPath("$[0].category").isEqualTo("DOCUMENTATION");
    }

    @Test
    void shouldReturnEmptyArrayInsteadOfNotFoundForEmptyMonth() {
        when(lufthansaService.getMonthlyTasks(anyInt(), anyInt())).thenReturn(Mono.just(List.of()));

        webTestClient.get().uri("/lufthansa/tasks?year=2026&month=9")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("[]");
    }

    @Test
    void shouldUpdateTask() {
        when(lufthansaService.updateTask(eq(7L), any())).thenReturn(Mono.just(TaskResponse.builder().id(7L).build()));

        webTestClient.put().uri("/lufthansa/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"createdAt": "2026-09-12", "category": "DOCUMENTATION", "description": "updated description"}
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.id").isEqualTo(7);

        final ArgumentCaptor<UpdateTaskRequest> request = ArgumentCaptor.forClass(UpdateTaskRequest.class);
        verify(lufthansaService).updateTask(eq(7L), request.capture());
        assertThat(request.getValue().getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(request.getValue().getCategory()).isEqualTo(TaskCategory.DOCUMENTATION);
    }

    @Test
    void shouldReturnBadRequestWhenUpdateIsInvalid() {
        webTestClient.put().uri("/lufthansa/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"createdAt": "2026-09-12", "category": "DOCUMENTATION", "description": "short"}
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(lufthansaService);
    }

    @Test
    void shouldReturnNotFoundWithBodyWhenUpdatedTaskDoesNotExist() {
        when(lufthansaService.updateTask(eq(7L), any())).thenReturn(Mono.error(new TaskNotFoundException(7L)));

        webTestClient.put().uri("/lufthansa/tasks/7")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"createdAt": "2026-09-12", "category": "DOCUMENTATION", "description": "updated description"}
                """)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.title").isEqualTo("Task not found")
            .jsonPath("$.description").isEqualTo("Task with id 7 does not exist");
    }

    @Test
    void shouldDeleteTask() {
        when(lufthansaService.deleteTask(7L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/lufthansa/tasks/7")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenDeletedTaskDoesNotExist() {
        when(lufthansaService.deleteTask(7L)).thenReturn(Mono.error(new TaskNotFoundException(7L)));

        webTestClient.delete().uri("/lufthansa/tasks/7")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.title").isEqualTo("Task not found");
    }

    @Test
    void shouldReportUnknownPathAsNotFoundInsteadOfServerError() {
        webTestClient.get().uri("/lufthansa/no-such-path")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void shouldReportUnsupportedMethodAsMethodNotAllowed() {
        webTestClient.patch().uri("/lufthansa/tasks/7")
            .exchange()
            .expectStatus().isEqualTo(405)
            .expectBody().jsonPath("$.status").isEqualTo(405);
    }

    @Test
    void shouldAcceptOptionalDateOnRegistration() {
        when(lufthansaService.registerTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder()
            .createdAt(LocalDate.of(2026, 9, 12))
            .build()));

        webTestClient.post().uri("/lufthansa/tasks:register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"description": "some valid description", "createdAt": "2026-09-12"}
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.createdAt").isEqualTo("2026-09-12");

        final ArgumentCaptor<CreateTaskRequest> request = ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(lufthansaService).registerTask(request.capture());
        assertThat(request.getValue().getCreatedAt()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(request.getValue().getDescription()).isEqualTo("some valid description");
    }
}
