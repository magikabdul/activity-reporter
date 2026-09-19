package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * Constraints placed directly on controller method parameters (the {@code year} / {@code month} query params).
 * Without it the generic ResponseStatusException handling answers "Validation failure" and nothing else.
 */
public class HandlerMethodValidationExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        HandlerMethodValidationException exception = (HandlerMethodValidationException) throwable;

        return ErrorMessage.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .title("invalid request content")
            .description(getParameterErrors(exception))
            .build();
    }

    private String getParameterErrors(final HandlerMethodValidationException exception) {
        return exception.getParameterValidationResults().stream()
            .flatMap(result -> result.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .map(message -> result.getMethodParameter().getParameterName() + " " + message))
            .collect(Collectors.joining(", "));
    }
}
