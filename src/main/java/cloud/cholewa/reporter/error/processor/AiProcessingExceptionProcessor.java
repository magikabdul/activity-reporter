package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.http.HttpStatus;

public class AiProcessingExceptionProcessor implements ExceptionProcessor{
    @Override
    public ErrorMessage process(final Throwable throwable) {
        return ErrorMessage.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .title("AI processing error")
            .description(throwable.getLocalizedMessage())
            .build();
    }
}
