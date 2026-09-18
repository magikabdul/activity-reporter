package cloud.cholewa.reporter.lufthansa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTaskRequest {
    @NotEmpty
    @Size(min = 10, max = 255)
    private String description;

    // day the work was done; today when omitted
    @Nullable
    private LocalDate createdAt;
}
