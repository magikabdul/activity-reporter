package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.error.AiUnavailableException;
import cloud.cholewa.reporter.trecom.model.ContentQualityResult;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class ContentQualityService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<ContentQualityResult> outputConverter;

    public ContentQualityService(final ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ContentQualityResult.class);
    }

    public Mono<Task> process(final CreateTaskRequest request, final Task task) {
        // the customer needs no AI, so a missing one is rejected before any tokens are spent
        return handleCustomer(request.getCustomer())
            .flatMap(customer -> checkContent(request)
                .map(result -> {
                    task.setHoursSpent(request.getHoursSpent());
                    task.setCustomer(customer);
                    task.setSalesmanFirstName(result.getFirstName());
                    task.setSalesmanLastName(result.getLastName());
                    task.setDescription(result.getDescription());
                    task.setNotes(resolveNotes(request.getNotes(), result.getNotes()));
                    return task;
                }))
            .doOnSubscribe(subscription -> log.info("Processing content quality for task with id: {}", task.getId()))
            .doOnError(throwable -> log.error(
                "Error processing content quality for task with id: {}, error: {}",
                task.getId(),
                throwable.getLocalizedMessage()
            ));
    }

    private Mono<String> handleCustomer(final String customer) {
        return Mono.justOrEmpty(customer)
            .doOnNext(c -> log.info("Processing customer: {}", c))
            .mapNotNull(StringUtils::upperCase)
            .switchIfEmpty(Mono.error(new AiProcessingException("Customer not provided")));
    }

    private Mono<ContentQualityResult> checkContent(final CreateTaskRequest request) {
        return Mono.fromCallable(() -> getContentQualityResult(request))
            .onErrorMap(
                e -> !(e instanceof AiProcessingException),
                e -> new AiUnavailableException("Failed to check the content of the task", e)
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    private ContentQualityResult getContentQualityResult(final CreateTaskRequest request) {
        final String firstName = request.getSalesman().getFirstName();
        final String lastName = request.getSalesman().getLastName();

        String response = chatClient
            .prompt(TrecomPrompt.buildPrompt(
                firstName, lastName, request.getDescription(), request.getNotes(), outputConverter))
            .call()
            .content();
        ContentQualityResult result = outputConverter.convert(response);

        if (result == null || StringUtils.isAnyBlank(
            result.getFirstName(), result.getLastName(), result.getDescription())) {
            throw new IllegalStateException("AI answer is missing the first name, the last name or the description");
        }

        log.info(
            "Task content was processed by AI, firstname [{}] -> [{}], lastname [{}] -> [{}], reasoning: {}",
            firstName, result.getFirstName(), lastName, result.getLastName(), result.getReasoning()
        );

        if (TrecomPrompt.NOT_A_NAME.equalsIgnoreCase(result.getFirstName())) {
            throw new AiProcessingException("Provided word is not a firstname: " + firstName);
        }
        if (TrecomPrompt.NOT_A_NAME.equalsIgnoreCase(result.getLastName())) {
            throw new AiProcessingException("Provided word is not a lastname: " + lastName);
        }
        return result;
    }

    // notes are optional: none given -> none stored, whatever AI put into the field
    private static String resolveNotes(final String requestedNotes, final String correctedNotes) {
        if (StringUtils.isBlank(requestedNotes)) {
            return null;
        }
        return StringUtils.defaultIfBlank(correctedNotes, requestedNotes);
    }
}
