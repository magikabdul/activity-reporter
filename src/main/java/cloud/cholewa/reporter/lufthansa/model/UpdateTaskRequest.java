package cloud.cholewa.reporter.lufthansa.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {
    @NotNull
    private LocalDate createdAt;

    @NotNull
    private TaskCategory category;

    // wider than on create: AI-corrected descriptions may already exceed the 255 chars accepted there
    @NotEmpty
    @Size(min = 10, max = 500)
    private String description;
}
