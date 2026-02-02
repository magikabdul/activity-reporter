package cloud.cholewa.reporter.error;

import cloud.cholewa.reporter.error.model.ErrorMessage;
import cloud.cholewa.reporter.error.processor.AiProcessingExceptionProcessor;
import cloud.cholewa.reporter.error.processor.DefaultExceptionProcessor;
import cloud.cholewa.reporter.error.processor.ExceptionProcessor;
import cloud.cholewa.reporter.error.processor.NotImplementedExceptionProcessor;
import cloud.cholewa.reporter.error.processor.TaskException;
import cloud.cholewa.reporter.error.processor.TaskExceptionProcessor;
import cloud.cholewa.reporter.error.processor.WebExchangeBindExceptionProcessor;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    private final Map<Class<? extends Exception>, ExceptionProcessor> processorMap;
    private final ExceptionProcessor defaultExceptionProcessor = new DefaultExceptionProcessor();

    public GlobalErrorWebExceptionHandler(
        final ErrorAttributes errorAttributes,
        final WebProperties.Resources resources,
        final ApplicationContext applicationContext,
        final ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());

        processorMap = Map.ofEntries(
            Map.entry(NotImplementedException.class, new NotImplementedExceptionProcessor()),
            Map.entry(WebExchangeBindException.class, new WebExchangeBindExceptionProcessor()),
            Map.entry(AiProcessingException.class, new AiProcessingExceptionProcessor()),
            Map.entry(TaskException.class, new TaskExceptionProcessor())
        );
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderedErrorResponse);
    }

    /*
     * Method overrode to not have a log for the parent method.
     * Only logging from the default processor is required
     */
    @Override
    @SuppressWarnings("java:S125")
    protected void logError(final ServerRequest request, final ServerResponse response, final Throwable throwable) {
        /* log.error("Error processing request [{}]: {}", request.uri(), throwable.getLocalizedMessage()); */
    }

    Mono<ServerResponse> renderedErrorResponse(final ServerRequest request) {
        final ErrorMessage errorMessage = getErrorMessage(Objects.requireNonNull(getError(request)));

        return ServerResponse
            .status(errorMessage.getStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorMessage));
    }

    private ErrorMessage getErrorMessage(final Throwable throwable) {
        return processorMap.getOrDefault(throwable.getClass(), defaultExceptionProcessor).process(throwable);
    }
}
