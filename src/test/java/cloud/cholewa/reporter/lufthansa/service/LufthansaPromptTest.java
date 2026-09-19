package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.CategorizationResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

class LufthansaPromptTest {

    private final BeanOutputConverter<CategorizationResult> outputConverter =
        new BeanOutputConverter<>(CategorizationResult.class);

    @Test
    void should_list_every_category_with_its_contract_wording() {
        String contents = LufthansaPrompt.buildPrompt("Opis zadania", outputConverter).getContents();

        assertThat(contents)
            .contains("- SOFTWARE_DEVELOPMENT: Projektowanie i testowanie oprogramowania")
            .contains("- ARCHITECTURE_DESIGN: Analiza i projektowanie architektury oprogramowania")
            .contains("- UNKNOWN: Nie można przypisać zadania")
            .contains("Opis zadania:", "Opis zadania")
            .doesNotContain("{categories}", "{description}", "{format}");
    }

    @Test
    void should_ask_for_the_closest_category_instead_of_unknown() {
        String contents = LufthansaPrompt.buildPrompt("Opis zadania", outputConverter).getContents();

        assertThat(contents)
            .contains("i tak wybierz najbliższą kategorię - nie zwracaj UNKNOWN")
            .contains("Kategorię UNKNOWN zwróć tylko wtedy");
    }

    @Test
    void should_ask_for_a_polish_reasoning_that_says_what_to_add() {
        String contents = LufthansaPrompt.buildPrompt("Opis zadania", outputConverter).getContents();

        assertThat(contents)
            .contains("w polu 'reasoning' zwróć w języku polskim")
            .contains("co konkretnie należy do niego dopisać")
            .contains("category", "description", "reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema");
    }

    @Test
    void should_keep_braces_in_user_text_literal() {
        assertThat(LufthansaPrompt.buildPrompt("Poprawka {format} w szablonie", outputConverter).getContents())
            .contains("Poprawka {format} w szablonie");
    }
}
