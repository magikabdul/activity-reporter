package cloud.cholewa.reporter.error;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import cloud.cholewa.reporter.error.processor.TaskNotFoundExceptionProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskNotFoundExceptionProcessorTest {

    @Test
    void shouldMapToNotFoundWithBody() {
        final ErrorMessage result = new TaskNotFoundExceptionProcessor().process(new TaskNotFoundException(42L));

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getTitle()).isEqualTo("Task not found");
        assertThat(result.getDescription()).isEqualTo("Task with id 42 does not exist");
    }
}
