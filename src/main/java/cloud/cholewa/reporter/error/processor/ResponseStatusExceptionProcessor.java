package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.web.server.ResponseStatusException;

/**
 * Framework errors that already carry their HTTP status (unknown path, unsupported method or media type…).
 * Without this they would fall through to the default processor and be reported as 500.
 */
public class ResponseStatusExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        ResponseStatusException exception = (ResponseStatusException) throwable;

        return ErrorMessage.builder()
            .status(exception.getStatusCode().value())
            .title(exception.getStatusCode().toString())
            .description(exception.getReason())
            .build();
    }
}
