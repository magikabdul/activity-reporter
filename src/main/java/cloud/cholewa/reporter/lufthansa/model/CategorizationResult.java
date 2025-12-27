package cloud.cholewa.reporter.lufthansa.model;

import lombok.Data;

@Data
public class CategorizationResult {
    private TaskCategory category;
    private String description;
    private String reasoning;
}
