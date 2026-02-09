package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.web.server.ServerWebInputException;

public class ServerWebInputExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        ServerWebInputException exception = (ServerWebInputException) throwable;

        return ErrorMessage.builder()
            .status(exception.getStatusCode().value())
            .title(exception.getReason())
            .description(exception.getMostSpecificCause().getMessage())
            .build();
    }
}
