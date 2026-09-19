package cloud.cholewa.reporter.trecom.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured answer of the single AI call that checks a whole Trecom task.
 * {@code firstName} / {@code lastName} carry the literal {@code false} when AI does not recognise the word as a name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentQualityResult {
    private String firstName;
    private String lastName;
    private String description;
    private String notes;
    private String reasoning;
}
