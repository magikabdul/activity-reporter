package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class TaskExceptionProcessor implements ExceptionProcessor{
    @Override
    public ErrorMessage process(final Throwable throwable) {
        log.error("Task processing error: {}", throwable.getLocalizedMessage());

        return ErrorMessage.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .title("Task processing error")
            .description(throwable.getLocalizedMessage())
            .build();
    }
}
