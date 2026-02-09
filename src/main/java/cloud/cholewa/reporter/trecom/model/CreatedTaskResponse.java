package cloud.cholewa.reporter.trecom.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "customer", "description", "hoursSpent", "salesman", "notes"})
public class CreatedTaskResponse {
    private UUID id;
    private String customer;
    private String description;
    private int hoursSpent;
    private Salesman salesman;
    private String notes;
}
