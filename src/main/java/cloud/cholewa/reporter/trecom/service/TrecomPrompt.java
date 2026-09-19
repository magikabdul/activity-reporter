package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.trecom.model.ContentQualityResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Map;

/**
 * One prompt for the whole task: the salesman's names, the description and the notes are checked in a single
 * OpenAI call (they used to be four calls, each repeating the JSON format instructions).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrecomPrompt {

    /** Value AI puts into firstName / lastName when the word is not a name. */
    public static final String NOT_A_NAME = "false";

    private static final String NO_NOTES = "(brak notatek)";

    public static Prompt buildPrompt(
        final String firstName,
        final String lastName,
        final String description,
        final String notes,
        final BeanOutputConverter<ContentQualityResult> outputConverter
    ) {
        return new PromptTemplate(TASK).create(Map.of(
            "firstname", firstName,
            "lastname", lastName,
            "description", description,
            "notes", StringUtils.defaultIfBlank(notes, NO_NOTES),
            "format", outputConverter.getFormat()
        ));
    }

    private static final String TASK = """
        Twoim zadaniem jest sprawdzenie czterech niezależnych elementów zadania: imienia, nazwiska, opisu i notatek.
        Każdy element oceniaj osobno, według reguł z jego sekcji.

        IMIĘ
        Określ czy podane imię jest poprawnym polskim imieniem.
        Jeżeli nie jesteś w stanie stwierdzić czy jest to polskie imię, sprawdź, czy podane słowo może być imieniem w innym języku.
        Zwróć poprawione imię jeżeli zawierało błąd np literówkę.
        Jeżeli nie jesteś w stanie określić, czy słowo jest imieniem, to zwróć - false.
        Każde imię powinno być pisane z wielkiej litery na początku i małymi literami w pozostałych częściach.

        NAZWISKO
        Określ czy podane nazwisko jest poprawnym polskim nazwiskiem.
        Jeżeli nie jesteś w stanie stwierdzić czy jest to polskie nazwisko, sprawdź, czy podane słowo może być nazwiskiem w innym języku.
        Zwróć poprawione nazwisko jeżeli zawierało błąd np literówkę.
        Jeżeli nie jesteś w stanie określić, czy słowo jest nazwiskiem, to zwróć - false.
        Jeżeli podane nazwisko, to Milosch, to jest to poprawne nazwisko i w tej formie należy je zwrócić.
        Każde nazwisko powinno być pisane z wielkiej litery na początku i małymi literami w pozostałych częściach.

        OPIS
        Określ czy podany opis jest zgodny z regułami języka polskiego.
        Jeżeli napotkasz błędy ortograficzne lub gramatyczne, popraw je i zwróć poprawiony opis.
        Nie wyróżniaj żadnego słowa, nie korzystaj z cudzysłowów, czy innych podobnych znaków.

        NOTATKI
        Określ czy podane notatki są zgodne z regułami języka polskiego.
        Jeżeli napotkasz błędy ortograficzne lub gramatyczne, popraw je i zwróć poprawione notatki.
        Używaj języka polskiego.
        Jeżeli zamiast notatek podano (brak notatek), zwróć pusty tekst.

        Nie dodawaj żadnych dodatkowych informacji, czy opisów poza strukturą JSON.
        W odpowiedzi JSON:
        - w polu 'firstName' zwróć poprawione imię (lub 'false' jeśli to nie imię)
        - w polu 'lastName' zwróć poprawione nazwisko (lub 'false' jeśli to nie nazwisko)
        - w polu 'description' zwróć poprawiony opis w języku polskim
        - w polu 'notes' zwróć poprawione notatki (lub pusty tekst, jeśli notatek nie podano)
        - w polu 'reasoning' zwróć opis wykonanych poprawek lub opis błędów w języku angielskim

        Podane imię:
        {firstname}

        Podane nazwisko:
        {lastname}

        Podany opis:
        {description}

        Podane notatki:
        {notes}

        {format}
        """;
}
