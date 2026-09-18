package cloud.cholewa.reporter.trecom.model;

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
@JsonPropertyOrder({"id", "createdAt", "customer", "description", "hoursSpent", "salesman", "notes"})
public class TaskResponse {
    private Long id;
    private LocalDate createdAt;
    private String customer;
    private String description;
    private int hoursSpent;
    private Salesman salesman;
    private String notes;
}
