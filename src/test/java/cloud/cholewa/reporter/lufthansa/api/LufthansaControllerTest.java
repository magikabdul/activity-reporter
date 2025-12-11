package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@WebFluxTest(LufthansaController.class)
class LufthansaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnBadRequestWhenDescriptionIsShorterThan10Chars() {
        webTestClient.post().uri("/lufthansa/tasks")
            .body(BodyInserters.fromValue(CreateTaskRequest.builder().description("Test").build()))
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnNotImplementedWhenDescriptionIsMin10CharsLength() {
        webTestClient.post()
            .uri("/lufthansa/tasks")
            .body(BodyInserters.fromValue(
                CreateTaskRequest.builder().description("Test description").build()
            ))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED)
            .expectBody(CreatedTaskResponse.class);
    }
}