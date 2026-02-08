package cloud.cholewa.reporter.trecom.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Task {
    private UUID id;
    private String customer;
    private String description;
    private int hoursSpent;
    private String salesmanFirstName;
    private String salesmanLastName;
    private String notes;
}
