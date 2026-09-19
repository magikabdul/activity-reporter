package cloud.cholewa.reporter.lufthansa.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class Task {
    private UUID id;
    private LocalDate createdAt;
    private String description;
    private TaskCategory category;
    /** Why AI picked this category - or, for UNKNOWN, what the description is missing. */
    private String reasoning;
}
