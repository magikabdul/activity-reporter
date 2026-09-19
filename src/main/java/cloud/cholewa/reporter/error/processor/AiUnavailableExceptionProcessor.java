package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.http.HttpStatus;

public class AiUnavailableExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        return ErrorMessage.builder()
            .status(HttpStatus.BAD_GATEWAY.value())
            .title("AI service error")
            .description(throwable.getLocalizedMessage())
            .build();
    }
}
