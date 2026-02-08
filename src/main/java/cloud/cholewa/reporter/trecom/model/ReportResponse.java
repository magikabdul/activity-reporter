package cloud.cholewa.reporter.trecom.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"createdAt", "company", "description", "salesman", "hoursSpent"})
public class ReportResponse {
    private LocalDate createdAt;
    private String company;
    private String description;
    private String salesman;
    private int hoursSpent;
}
