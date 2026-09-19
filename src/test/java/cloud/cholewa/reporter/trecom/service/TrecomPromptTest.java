package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.trecom.model.ContentQualityResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

class TrecomPromptTest {

    private final BeanOutputConverter<ContentQualityResult> outputConverter =
        new BeanOutputConverter<>(ContentQualityResult.class);

    @Test
    void should_put_every_part_of_the_task_into_one_prompt() {
        Prompt prompt = TrecomPrompt.buildPrompt(
            "Jan", "Kowalski", "To jest przykladowy opis.", "To sa notatki do poprawy.", outputConverter);

        assertThat(prompt.getContents())
            .contains("Podane imię:", "Jan")
            .contains("Podane nazwisko:", "Kowalski")
            .contains("Podany opis:", "To jest przykladowy opis.")
            .contains("Podane notatki:", "To sa notatki do poprawy.")
            .doesNotContain("{firstname}", "{lastname}", "{description}", "{notes}", "{format}");
    }

    @Test
    void should_keep_the_rules_of_the_former_separate_prompts() {
        String contents = TrecomPrompt.buildPrompt("Jan", "Milosch", "Opis zadania", null, outputConverter)
            .getContents();

        assertThat(contents)
            .contains("Milosch, to jest to poprawne nazwisko")
            .contains("z wielkiej litery na początku")
            .contains("zwróć - false")
            .contains("nie korzystaj z cudzysłowów")
            .contains("Używaj języka polskiego");
    }

    @Test
    void should_describe_the_json_answer() {
        String contents = TrecomPrompt.buildPrompt("Jan", "Kowalski", "Opis zadania", "Notatki", outputConverter)
            .getContents();

        assertThat(contents)
            .contains("firstName", "lastName", "description", "notes", "reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema");
    }

    @Test
    void should_tell_ai_that_no_notes_were_given() {
        // \R: the template renderer writes the platform line separator
        assertThat(TrecomPrompt.buildPrompt("Jan", "Kowalski", "Opis zadania", null, outputConverter).getContents())
            .containsPattern("Podane notatki:\\R\\(brak notatek\\)");
        assertThat(TrecomPrompt.buildPrompt("Jan", "Kowalski", "Opis zadania", "  ", outputConverter).getContents())
            .containsPattern("Podane notatki:\\R\\(brak notatek\\)");
    }

    @Test
    void should_keep_braces_in_user_text_literal() {
        assertThat(TrecomPrompt.buildPrompt("Jan", "Kowalski", "Poprawka {format} w szablonie", null, outputConverter)
            .getContents())
            .contains("Poprawka {format} w szablonie");
    }
}
