package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class DefaultExceptionProcessor implements ExceptionProcessor {

    @Override
    public ErrorMessage process(final Throwable throwable) {
        log.error(
            "Generic processing error for exception [{}]: {}",
            throwable.getClass().getName(),
            throwable.getLocalizedMessage()
        );

        return ErrorMessage.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .title(throwable.getLocalizedMessage())
            .description("Exception of type " + throwable.getClass().getSimpleName() + " occurred")
            .build();
    }
}
