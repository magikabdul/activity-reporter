package cloud.cholewa.reporter.lufthansa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "createdAt", "category", "description", "reasoning"})
public class CreatedTaskResponse {
    private UUID id;
    private LocalDate createdAt;
    private TaskCategory category;
    private String description;
    /** Only on register: why AI picked this category, or what the description is missing when it is UNKNOWN. */
    private String reasoning;
}
