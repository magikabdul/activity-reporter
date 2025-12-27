package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.ReportResponse;
import cloud.cholewa.reporter.lufthansa.service.LufthansaReportService;
import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(LufthansaController.class)
class LufthansaControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private LufthansaService lufthansaService;
    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private LufthansaReportService lufthansaReportService;

    @Test
    void shouldReturnBadRequestWhenDescriptionIsShorterThan10Chars() {
        webTestClient.post().uri("/lufthansa/tasks:register")
            .body(BodyInserters.fromValue(CreateTaskRequest.builder().description("Test").build()))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnOKWhenDescriptionIsMin10CharsLength() {
        when(lufthansaService.registerTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder().build()));

        webTestClient.post()
            .uri("/lufthansa/tasks:register")
            .body(BodyInserters.fromValue(
                CreateTaskRequest.builder().description("Test description").build()
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(CreatedTaskResponse.class);
    }

    @Test
    void shouldCompleteTask() {
        when(lufthansaService.completeTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder().build()));

        webTestClient.post().uri("/lufthansa/tasks:complete/12345678-1234-1234-1234-123456789abc")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldRespondWithBadRequest_whenTaskIdIsInvalid() {
        when(lufthansaService.completeTask(any())).thenReturn(Mono.error(new TaskException("Invalid UUID: invalid-id")));

        webTestClient.post().uri("/lufthansa/tasks:complete/invalid-id")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldResponseWithReport() {
        when(lufthansaReportService.getMonthlyReport(anyInt(), anyInt()))
            .thenReturn(Mono.just(List.of(ReportResponse.builder().build())));

        webTestClient.get().uri("/lufthansa/report?year=2022&month=01")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void shouldResponseWithBadRequestWhenYearAndMonthNotProvided() {

        webTestClient.get().uri("/lufthansa/report")
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldResponseNoContentWhenRaportIsEmpty() {
        when(lufthansaReportService.getMonthlyReport(anyInt(), anyInt()))
            .thenReturn(Mono.empty());

        webTestClient.get().uri("/lufthansa/report?year=2022&month=01")
            .exchange()
            .expectStatus().isNoContent();
    }
}