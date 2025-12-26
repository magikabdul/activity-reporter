package cloud.cholewa.reporter.error.processor;

import cloud.cholewa.reporter.error.model.ErrorMessage;

@FunctionalInterface
public interface ExceptionProcessor {
    ErrorMessage process(final Throwable throwable);
}
