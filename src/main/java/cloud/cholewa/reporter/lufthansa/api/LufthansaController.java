package cloud.cholewa.reporter.lufthansa.api;

import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import cloud.cholewa.reporter.lufthansa.model.ReportResponse;
import cloud.cholewa.reporter.lufthansa.service.LufthansaReportService;
import cloud.cholewa.reporter.lufthansa.service.LufthansaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController()
@RequestMapping("/lufthansa")
public class LufthansaController {

    private final LufthansaService lufthansaService;
    private final LufthansaReportService lufthansaReportService;

    @PostMapping("/tasks:register")
    Mono<ResponseEntity<CreatedTaskResponse>> registerTask(@Valid @RequestBody CreateTaskRequest request) {
        return lufthansaService.registerTask(request)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Creating task for Lufthansa"));
    }

    @PostMapping("/tasks:complete/{taskId}")
    Mono<ResponseEntity<CreatedTaskResponse>> completeTask(@PathVariable UUID taskId) {
        return lufthansaService.completeTask(taskId)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Completing task for Lufthansa with id: {}", taskId));
    }

    @GetMapping("/report")
    Mono<ResponseEntity<List<ReportResponse>>> getMonthlyReport(
        @RequestParam(name = "year") int year,
        @RequestParam(name = "month") int month
    ) {
        return lufthansaReportService.getMonthlyReport(year, month)
            .map(ResponseEntity::ok)
            .switchIfEmpty(Mono.just(ResponseEntity.noContent().build()))
            .doOnSubscribe(subscription -> log.info(
                "Retrieving monthly report for Lufthansa for year: {}, month: {}",
                year,
                month
            ));
    }
}
