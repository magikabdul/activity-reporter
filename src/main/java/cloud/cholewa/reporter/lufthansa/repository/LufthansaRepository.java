package cloud.cholewa.reporter.lufthansa.repository;

import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface LufthansaRepository extends R2dbcRepository<TaskEntity, Long> {

    @Query("SELECT * FROM lufthansa WHERE category= :category AND created_at >= DATE_TRUNC('month', :date) AND created_at < DATE_TRUNC('month', :date) + INTERVAL '1 month'")
    Flux<TaskEntity> findAllByByDateAndCategory(final LocalDate date, final TaskCategory category);
}
