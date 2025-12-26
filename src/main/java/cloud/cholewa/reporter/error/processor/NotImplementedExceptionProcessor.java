package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.http.HttpStatus;

public class NotImplementedExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        return ErrorMessage.builder()
            .status(HttpStatus.NOT_IMPLEMENTED.value())
            .title("Not implemented")
            .description("Method not implemented yet")
            .build();
    }
}
