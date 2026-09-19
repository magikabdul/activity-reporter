package cloud.cholewa.reporter.error;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import cloud.cholewa.reporter.error.processor.AiUnavailableExceptionProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiUnavailableExceptionProcessorTest {

    @Test
    void shouldMapToBadGatewayWithBody() {
        final ErrorMessage result = new AiUnavailableExceptionProcessor().process(
            new AiUnavailableException("Failed to categorize the task", new RuntimeException("timeout")));

        assertThat(result.getStatus()).isEqualTo(502);
        assertThat(result.getTitle()).isEqualTo("AI service error");
        assertThat(result.getDescription()).isEqualTo("Failed to categorize the task");
    }
}
