package cloud.cholewa.reporter.trecom.api;

import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.CreatedTaskResponse;
import cloud.cholewa.reporter.trecom.model.ReportResponse;
import cloud.cholewa.reporter.trecom.model.TaskResponse;
import cloud.cholewa.reporter.trecom.model.UpdateTaskRequest;
import cloud.cholewa.reporter.trecom.service.TrecomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("trecom")
public class TrecomController {

    private final TrecomService trecomService;

    @PostMapping("/tasks:register")
    Mono<ResponseEntity<CreatedTaskResponse>> registerTask(@Valid @RequestBody final CreateTaskRequest request) {
        return trecomService.registerTask(request)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Incoming task registration request received"));
    }

    @PostMapping("tasks:complete/{taskId}")
    Mono<ResponseEntity<CreatedTaskResponse>> completeTask(@PathVariable final UUID taskId) {
        return trecomService.completeTask(taskId)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Incoming task completion request received"));
    }

    @GetMapping("/report")
    Mono<ResponseEntity<List<ReportResponse>>> getReport(
        @RequestParam(name = "year") int year,
        @RequestParam(name = "month") int month
    ) {
        return trecomService.getMonthlyReport(year, month)
            .filter(raportList -> !raportList.isEmpty())
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Incoming report request received"))
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/tasks")
    Mono<ResponseEntity<List<TaskResponse>>> getMonthlyTasks(
        @RequestParam(name = "year") int year,
        @RequestParam(name = "month") int month
    ) {
        return trecomService.getMonthlyTasks(year, month)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info(
                "Retrieving Trecom tasks for year: {}, month: {}", year, month));
    }

    @PutMapping("/tasks/{taskId}")
    Mono<ResponseEntity<TaskResponse>> updateTask(
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        return trecomService.updateTask(taskId, request)
            .map(ResponseEntity::ok)
            .doOnSubscribe(subscription -> log.info("Updating Trecom task with id: {}", taskId));
    }

    @DeleteMapping("/tasks/{taskId}")
    Mono<ResponseEntity<Void>> deleteTask(@PathVariable Long taskId) {
        return trecomService.deleteTask(taskId)
            .thenReturn(ResponseEntity.noContent().<Void>build())
            .doOnSubscribe(subscription -> log.info("Deleting Trecom task with id: {}", taskId));
    }
}
