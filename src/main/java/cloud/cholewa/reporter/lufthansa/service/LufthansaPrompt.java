package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.CategorizationResult;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The prompt that assigns a contractual work category and corrects the Polish wording.
 * UNKNOWN is deliberately hard to reach: a task that concerns software work but is described in general terms
 * still gets the closest category, and 'reasoning' has to tell the user what to add when it does not.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LufthansaPrompt {

    public static Prompt buildPrompt(
        final String description,
        final BeanOutputConverter<CategorizationResult> outputConverter
    ) {
        return new PromptTemplate(TASK).create(Map.of(
            "categories", categoriesWithDescriptions(),
            "description", description,
            "format", outputConverter.getFormat()
        ));
    }

    private static String categoriesWithDescriptions() {
        return Arrays.stream(TaskCategory.values())
            .map(category -> String.format("- %s: %s", category.name(), category.getDescription()))
            .collect(Collectors.joining("\n"));
    }

    private static final String TASK = """
        Twoim zadaniem jest przypisanie opisu zadania do jednej z dostępnych kategorii, uzasadnienie wyboru
        oraz poprawienie opisu zadania.

        WYBÓR KATEGORII
        Nie wymyślaj innych kategorii niż podane.
        Jeżeli opis zadania pasuje do wielu kategorii, zwróć najbardziej pasującą.
        Jeżeli opis zadania dotyczy pracy przy wytwarzaniu oprogramowania, ale jest ogólny lub nie wymienia
        wprost czynności z opisu kategorii, to i tak wybierz najbliższą kategorię - nie zwracaj UNKNOWN.
        Kategorię UNKNOWN zwróć tylko wtedy, gdy opis zadania w ogóle nie dotyczy pracy przy oprogramowaniu
        albo nie zawiera żadnych istotnych informacji.

        OPIS ZADANIA
        Jeżeli w opisie zadania pojawią się błędy z punktu widzenia języka polskiego, to je popraw.
        Poprawiony opis zadania ma być logiczny i zgodny z regułami języka polskiego, nie może zawierać błędów
        gramatycznych, ani ortograficznych.
        Poprawiony opis zwróć zawsze, także wtedy, gdy kategorią jest UNKNOWN.

        Nie dodawaj żadnych dodatkowych informacji, czy opisów poza strukturą JSON.
        W odpowiedzi JSON:
        - w polu 'category' zwróć nazwę wybranej kategorii
        - w polu 'description' zwróć poprawiony opis zadania w języku polskim
        - w polu 'reasoning' zwróć w języku polskim jedno lub dwa zdania uzasadnienia wyboru kategorii;
          jeżeli kategorią jest UNKNOWN, napisz czego brakuje w opisie i co konkretnie należy do niego dopisać,
          aby dało się przypisać zadanie do jednej z kategorii

        Dostępne kategorie:
        {categories}

        Opis zadania:
        {description}

        {format}
        """;
}
