package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController()
@RequestMapping("/lufthansa")
public class LufthansaController {

    private final LufthansaService lufthansaService;

    @PostMapping("/tasks:register")
    public Mono<ResponseEntity<CreatedTaskResponse>> registerTask(@Valid @RequestBody CreateTaskRequest request) {
        return lufthansaService.registerTask(request)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Creating task for Lufthansa"));
    }

    @PostMapping("/tasks:complete/{taskId}")
    public Mono<CreatedTaskResponse> completeTask(@PathVariable UUID taskId) {
        return lufthansaService.completeTask(taskId)
            .doOnSubscribe(subscription -> log.info("Completing task for Lufthansa with id: {}", taskId));
    }
}
