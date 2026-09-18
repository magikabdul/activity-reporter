package cloud.cholewa.reporter.lufthansa.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "createdAt", "category", "description"})
public class TaskResponse {
    private Long id;
    private LocalDate createdAt;
    private TaskCategory category;
    private String description;
}
