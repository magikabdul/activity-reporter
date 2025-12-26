package cloud.cholewa.reporter.lufthansa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("lufthansa")
public class TaskEntity {

    @Id
    private Long id;

    private LocalDate createdAt;

    private String description;

    private TaskCategory category;

}
