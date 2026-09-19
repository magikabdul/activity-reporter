package cloud.cholewa.reporter.trecom.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTaskRequest {
    @NotNull
    private String customer;
    @NotNull
    @Size(min = 10, max = 255)
    private String description;
    @Min(1)
    private int hoursSpent;
    @Valid
    @NotNull
    private Salesman salesman;
    // sent to OpenAI as it is, so it needs a ceiling
    @Nullable
    @Size(max = 2000)
    private String notes;
    // day the work was done; today when omitted
    @Nullable
    private LocalDate createdAt;
}
