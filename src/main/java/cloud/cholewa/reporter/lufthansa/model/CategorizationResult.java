package cloud.cholewa.reporter.lufthansa.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class CategorizationResult {

    @JsonPropertyDescription("Nazwa jednej z dostępnych kategorii, albo UNKNOWN gdy nie da się wybrać żadnej")
    private TaskCategory category;

    @JsonPropertyDescription("Poprawiony opis zadania w języku polskim")
    private String description;

    @JsonPropertyDescription(
        "Uzasadnienie wyboru kategorii w języku polskim; dla UNKNOWN - czego brakuje w opisie i co dopisać")
    private String reasoning;
}
