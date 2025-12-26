package cloud.cholewa.reporter.lufthansa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTaskRequest {
    @NotEmpty
    @Size(min = 10, max = 255)
    private String description;
}
