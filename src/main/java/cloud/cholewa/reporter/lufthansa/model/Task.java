package cloud.cholewa.reporter.lufthansa.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Task {
    private UUID id;
    private String description;
    private TaskCategory category;
}
