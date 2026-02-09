package cloud.cholewa.reporter.trecom.api;

import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.Salesman;
import cloud.cholewa.reporter.trecom.service.TrecomService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(TrecomController.class)
class TrecomControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private TrecomService trecomService;

    @Test
    void should_register_task() {
        Salesman salesman = new Salesman();
        salesman.setFirstName("Test");
        salesman.setLastName("User");

        CreateTaskRequest request = new CreateTaskRequest();
        request.setCustomer("Test customer");
        request.setDescription("Test description");
        request.setHoursSpent(10);
        request.setSalesman(salesman);

        when(trecomService.registerTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder().build()));

        webTestClient.post().uri("/trecom/tasks:register")
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_complete_task() {
        when(trecomService.completeTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder().build()));

        webTestClient.post().uri("/trecom/tasks:complete/12345678-1234-1234-1234-123456789abc")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_get_monthly_report() {
        when(trecomService.getMonthlyReport(anyInt(), anyInt())).thenReturn(Mono.just(List.of(new ReportResponse())));

        webTestClient.get().uri("/trecom/report?year=2022&month=01")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void should_get_not_found_monthly_report() {
        when(trecomService.getMonthlyReport(anyInt(), anyInt())).thenReturn(Mono.just(Collections.emptyList()));

        webTestClient.get().uri("/trecom/report?year=2022&month=01")
            .exchange()
            .expectStatus().isNotFound();
    }
}