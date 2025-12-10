package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@WebFluxTest(LufthansaController.class)
class LufthansaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldCreateTaskReturnNotImplemented() {
        webTestClient.post().uri("/lufthansa/tasks")
            .body(BodyInserters.fromValue(CreateTaskRequest.builder().description("Test").build()))
            .exchange()
            .expectStatus().is4xxClientError();
    }

}