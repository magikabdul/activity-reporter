package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.trecom.model.ChatResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrecomPrompt {

    public static final String FIRSTNAME_TEXT = "firstname";
    public static final String LASTNAME_TEXT = "lastname";
    public static final String DESCRIPTION_TEXT = "description";
    public static final String NOTES_TEXT = "notes";

    public static Prompt buildPrompt(
        final String textType,
        final String text,
        final BeanOutputConverter<ChatResponse> outputConverter
    ) {
        return new PromptTemplate(getPrompt(textType))
            .create(Map.of(textType, text, "format", outputConverter.getFormat()));
    }
    
    private static String getPrompt(final String textType) {
        return switch (textType) {
            case FIRSTNAME_TEXT -> FIRST_NAME;
            case LASTNAME_TEXT -> LAST_NAME;
            case DESCRIPTION_TEXT -> DESCRIPTION;
            case NOTES_TEXT -> NOTES;
            default -> throw new IllegalArgumentException("Unsupported text type: " + textType);
        };
    }

    private static final String FIRST_NAME = """
        Twoim zadaniem jest określić czy podane imię jest poprawnym polskim imieniem.
        Jeżeli nie jesteś w stanie stwierdzić czy jest to polskie imię, sprawdź, czy podane słowo może być imieniem w innym języku.
        Zwróć poprawione imię jeżeli zawierało błąd np literówkę.
        Jeżeli nie jesteś w stanie określić, czy słowo jest imieniem, to zwróć - false.
        Każde imię powinno być pisane z wielkiej litery na początku i małymi literami w pozostałych częściach.
        
        W odpowiedzi JSON:
        - w polu 'message' zwróć poprawione imię (lub 'false' jeśli to nie imię)
        - w polu 'reasoning' zwróć opis wykonanych poprawek lub opis błędu w języku angielskim
        
        Podane słowo:
        {firstname}
        
        {format}
        """;
    private static final String LAST_NAME = """
        Twoim zadaniem jest określić czy podane nazwisko jest poprawnym polskim nazwiskiem.
        Jeżeli nie jesteś w stanie stwierdzić czy jest to polskie nazwisko, sprawdź, czy podane słowo może być nazwiskiem w innym języku.
        Zwróć poprawione nazwisko jeżeli zawierało błąd np literówkę.
        Jeżeli nie jesteś w stanie określić, czy słowo jest nazwiskiem, to zwróć - false.
        Jeżeli podane nazwisko, to Milosch, to jest to poprawne nazwisko i w tej formie należy je zwrócić.
        Każde nazwisko powinno być pisane z wielkiej litery na początku i małymi literami w pozostałych częściach.
        
        W odpowiedzi JSON:
        - w polu 'message' zwróć poprawione nazwisko (lub 'false' jeśli to nie nazwisko)
        - w polu 'reasoning' zwróć opis wykonanych poprawek lub opis błędu w języku angielskim
        
        Podane słowo:
        {lastname}
        
        {format}
        """;
    private static final String DESCRIPTION = """
        Twoim zadaniem jest określić czy podany opis jest zgodny z regułami języka polskiego.
        Jeżeli napotkasz błędy ortograficzne lub gramatyczne, popraw je i zwróć poprawiony opis.
        Nie wyróżniaj żadnego słowa, nie korzystaj z cudzysłowów, czy innych podobnych znaków.
        
        W odpowiedzi JSON:
        - w polu 'message' zwróć poprawiony opis w języku polskim
        - w polu 'reasoning' zwróć opis błędu lub opis wykonanych zmian w języku angielskim
        
        Podany opis:
        {description}
        
        {format}
        """;
    private static final String NOTES = """
        Twoim zadaniem jest określić czy podane notatki są zgodne z regułami języka polskiego.
        Jeżeli napotkasz błędy ortograficzne lub gramatyczne, popraw je i zwróć poprawione notatki.
        Używaj języka polskiego.
        Nie dodawaj żadnych dodatkowych informacji, czy opisów poza strukturą JSON.
        
        W odpowiedzi JSON:
        - w polu 'message' zwróć poprawione notatki
        - w polu 'reasoning' zwróć opis błędu lub opis wykonanych zmian w języku angielskim
        
        Podane notatki:
        {notes}
        
        {format}
        """;
}
