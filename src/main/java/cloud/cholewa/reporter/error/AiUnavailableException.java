package cloud.cholewa.reporter.error;

/**
 * The AI call itself failed (timeout, 5xx from OpenAI, unreadable answer) - as opposed to
 * {@link AiProcessingException}, where AI answered and rejected the input.
 */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
