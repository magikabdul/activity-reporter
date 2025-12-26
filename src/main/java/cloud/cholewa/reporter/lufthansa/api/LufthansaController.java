package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController()
@RequestMapping("/lufthansa")
public class LufthansaController {

    private final LufthansaService lufthansaService;

    @PostMapping("/tasks")
    public Mono<ResponseEntity<CreatedTaskResponse>> registerTask(@Valid @RequestBody CreateTaskRequest request) {
        return lufthansaService.registerTask(request)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Creating task for Lufthansa"));
    }
}
