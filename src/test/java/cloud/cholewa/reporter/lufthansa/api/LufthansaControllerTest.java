package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(LufthansaController.class)
class LufthansaControllerTest {

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean(answers = Answers.RETURNS_SMART_NULLS)
    private LufthansaService lufthansaService;

    @Test
    void shouldReturnBadRequestWhenDescriptionIsShorterThan10Chars() {
        webTestClient.post().uri("/lufthansa/tasks")
            .body(BodyInserters.fromValue(CreateTaskRequest.builder().description("Test").build()))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnOKWhenDescriptionIsMin10CharsLength() {
        when(lufthansaService.registerTask(any())).thenReturn(Mono.just(CreatedTaskResponse.builder().build()));

        webTestClient.post()
            .uri("/lufthansa/tasks")
            .body(BodyInserters.fromValue(
                CreateTaskRequest.builder().description("Test description").build()
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(CreatedTaskResponse.class);
    }
}