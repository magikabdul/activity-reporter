package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Optional;
import java.util.stream.Collectors;

public class WebExchangeBindExceptionProcessor implements ExceptionProcessor {
    @Override
    public ErrorMessage process(final Throwable throwable) {
        WebExchangeBindException exception = (WebExchangeBindException) throwable;

        return ErrorMessage.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .title(StringUtils.lowerCase(Strings.CS.removeEnd(exception.getBody().getDetail(), ".")))
            .description(getFieldErrors(exception))
            .build();
    }

    private String getFieldErrors(final WebExchangeBindException exception) {
        return exception.getAllErrors().stream()
            .map(objectError -> extractName(objectError) + " " + extractValue(objectError))
            .collect(Collectors.joining(", "));
    }

    private static String extractName(final ObjectError objectError) {
        if (objectError.getArguments().length > 0) {
            return StringUtils.lowerCase(Optional.of(objectError.getArguments()[0])
                .filter(DefaultMessageSourceResolvable.class::isInstance)
                .map(DefaultMessageSourceResolvable.class::cast)
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(""));
        }
        return "";
    }

    private static String extractValue(final ObjectError objectError) {
        return objectError.getDefaultMessage();
    }
}
