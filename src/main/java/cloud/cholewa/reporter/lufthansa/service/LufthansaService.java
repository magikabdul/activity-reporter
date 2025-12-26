package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.mapper.TaskMapper;
import cloud.cholewa.reporter.lufthansa.model.CreateTaskRequest;
import cloud.cholewa.reporter.lufthansa.model.CreatedTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LufthansaService {

    private final CategorizeService categorizeService;
//    private final LufthansaRepository lufthansaRepository;
    private final TaskMapper taskMapper;

    public Mono<CreatedTaskResponse> registerTask(final CreateTaskRequest request) {
        return categorizeService.categorize(request.getDescription())
            .map(taskCategory -> taskMapper.toEntity(request, taskCategory))
//            .flatMap(lufthansaRepository::save)
            .doOnNext(task -> log.info("Saving task with id: {}", task.getId()))
            .map(taskMapper::toResponse);
    }
}
