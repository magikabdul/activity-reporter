package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.http.HttpStatus;

public class TaskNotFoundExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        return ErrorMessage.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .title("Task not found")
            .description(throwable.getLocalizedMessage())
            .build();
    }
}
