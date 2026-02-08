package cloud.cholewa.reporter.trecom.repository;

import cloud.cholewa.reporter.trecom.model.TaskEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface TrecomRepository extends R2dbcRepository<TaskEntity, Long> {

    @Query("SELECT * FROM trecom WHERE created_at >= date_trunc('month', :date) AND created_at < date_trunc('month', :date) + interval '1 month'")
    Flux<TaskEntity> findAllByDateRange(LocalDate date);
}
