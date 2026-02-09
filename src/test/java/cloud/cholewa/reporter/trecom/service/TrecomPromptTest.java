package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.trecom.model.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;

import static org.assertj.core.api.Assertions.assertThat;

class TrecomPromptTest {

    @Test
    void should_build_prompt_for_firstname_text_type() {
        String firstname = "Jan";
        BeanOutputConverter<ChatResponse> outputConverter = new BeanOutputConverter<>(ChatResponse.class);

        Prompt prompt = TrecomPrompt.buildPrompt(TrecomPrompt.FIRSTNAME_TEXT, firstname, outputConverter);

        String contents = prompt.getContents();
        assertThat(contents)
            .contains("imię")
            .contains("Podane")
            .contains(firstname)
            .contains("message")
            .contains("reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema")
            .doesNotContain("{firstname}");
    }

    @Test
    void should_build_prompt_for_lastname_text_type() {
        String lastname = "Kowalski";
        BeanOutputConverter<ChatResponse> outputConverter = new BeanOutputConverter<>(ChatResponse.class);

        Prompt prompt = TrecomPrompt.buildPrompt(TrecomPrompt.LASTNAME_TEXT, lastname, outputConverter);

        String contents = prompt.getContents();
        assertThat(contents)
            .contains("nazwisko")
            .contains("Milosch")
            .contains("Podane")
            .contains(lastname)
            .contains("message")
            .contains("reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema")
            .doesNotContain("{lastname}");
    }

    @Test
    void should_build_prompt_for_description_text_type() {
        String description = "To jest przykladowy opis.";
        BeanOutputConverter<ChatResponse> outputConverter = new BeanOutputConverter<>(ChatResponse.class);

        Prompt prompt = TrecomPrompt.buildPrompt(TrecomPrompt.DESCRIPTION_TEXT, description, outputConverter);

        String contents = prompt.getContents();
        assertThat(contents)
            .contains("opis")
            .contains("Podany")
            .contains(description)
            .contains("message")
            .contains("reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema")
            .doesNotContain("{description}");
    }

    @Test
    void should_build_prompt_for_notes_text_type() {
        String notes = "To sa notatki do poprawy.";
        BeanOutputConverter<ChatResponse> outputConverter = new BeanOutputConverter<>(ChatResponse.class);

        Prompt prompt = TrecomPrompt.buildPrompt(TrecomPrompt.NOTES_TEXT, notes, outputConverter);

        String contents = prompt.getContents();
        assertThat(contents)
            .contains("notatki")
            .contains("Podane")
            .contains(notes)
            .contains("message")
            .contains("reasoning")
            .contains("https://json-schema.org/draft/2020-12/schema")
            .doesNotContain("{notes}");
    }

    @Test
    void should_throw_illegal_argument_exception_for_unsupported_text_type() {
        String textType = "unsupported";
        String text = "sample";
        BeanOutputConverter<ChatResponse> outputConverter = new BeanOutputConverter<>(ChatResponse.class);

        assertThat(org.assertj.core.api.Assertions
            .catchThrowable(() -> TrecomPrompt.buildPrompt(textType, text, outputConverter)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported text type: " + textType);
    }

    @Test
    void should_define_expected_text_type_constants() {
        assertThat(TrecomPrompt.FIRSTNAME_TEXT).isEqualTo("firstname");
        assertThat(TrecomPrompt.LASTNAME_TEXT).isEqualTo("lastname");
        assertThat(TrecomPrompt.DESCRIPTION_TEXT).isEqualTo("description");
        assertThat(TrecomPrompt.NOTES_TEXT).isEqualTo("notes");
    }
}
