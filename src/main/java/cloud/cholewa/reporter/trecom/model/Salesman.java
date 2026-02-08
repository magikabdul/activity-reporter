package cloud.cholewa.reporter.trecom.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Salesman {
    
    @NotEmpty
    @Pattern(
        regexp = "^[A-Z\\p{Lu}][a-z\\p{Ll}]*$",
        message = "First name must start with an uppercase letter and contain only letters"
    )
    private String firstName;
    
    @NotEmpty
    @Pattern(
        regexp = "^[A-Z\\p{Lu}][a-z\\p{Ll}]*(-[A-Z\\p{Lu}][a-z\\p{Ll}]*)?$",
        message = "Last name must start with an uppercase letter and contain only letters (hyphenated names allowed)")
    private String lastName;
}
