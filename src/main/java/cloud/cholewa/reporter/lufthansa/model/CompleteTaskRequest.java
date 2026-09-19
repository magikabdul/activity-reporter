package cloud.cholewa.reporter.lufthansa.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body of tasks:complete - the category the user picked by hand, needed when AI answered UNKNOWN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {

    @NotNull
    private TaskCategory category;
}
