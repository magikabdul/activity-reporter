package cloud.cholewa.reporter.trecom.model;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    @Size(max = 100)
    private String customer;

    // wider than on create: AI-corrected descriptions may already exceed the 255 chars accepted there
    @NotEmpty
    @Size(min = 10, max = 500)
    private String description;

    @Min(1)
    private int hoursSpent;

    @Valid
    @NotNull
    private Salesman salesman;

    @Nullable
    private String notes;
}
