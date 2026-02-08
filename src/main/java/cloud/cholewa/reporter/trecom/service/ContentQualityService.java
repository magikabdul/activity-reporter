package cloud.cholewa.reporter.trecom.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.trecom.model.ChatResponse;
import cloud.cholewa.reporter.trecom.model.CreateTaskRequest;
import cloud.cholewa.reporter.trecom.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

@Slf4j
@Service
public class ContentQualityService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<ChatResponse> outputConverter;

    public ContentQualityService(final ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ChatResponse.class);
    }

    public Mono<Task> process(final CreateTaskRequest request, final Task task) {
        return Mono.zip(
                handleCustomer(request.getCustomer()),
                handleFirstname(request.getSalesman().getFirstName()),
                handleLastname(request.getSalesman().getLastName()),
                handleDescription(request.getDescription())
            )
            .map(tuples -> {
                task.setHoursSpent(request.getHoursSpent());
                task.setCustomer(tuples.getT1());
                task.setSalesmanFirstName(tuples.getT2());
                task.setSalesmanLastName(tuples.getT3());
                task.setDescription(tuples.getT4());
                return task;
            })
            .map(t -> {
                Optional.ofNullable(request.getNotes()).ifPresent(notes -> handleNotes(notes).subscribe());
                return t;
            })
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

    private Mono<String> handleFirstname(final String firstname) {
        return chatClient
            .prompt(TrecomPrompt.buildPrompt(TrecomPrompt.FIRSTNAME_TEXT, firstname, outputConverter))
            .stream()
            .content()
            .doOnSubscribe(subscription -> log.info("Processing firstname: {}", firstname))
            .collectList()
            .map(list -> StringUtils.join(list, ""))
            .mapNotNull(outputConverter::convert)
            .doOnNext(chatResponse -> log.info(
                "Firstname [{}], were processed by AI, the output is: [{}] with reasoning: {}",
                firstname,
                chatResponse.getMessage(),
                chatResponse.getReasoning()
            ))
            .flatMap(chatResponse ->
                "false".equalsIgnoreCase(chatResponse.getMessage())
                    ? Mono.error(new AiProcessingException("Provided word is not a firstname: " + firstname))
                    : Mono.just(chatResponse.getMessage()))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> handleLastname(final String lastname) {
        return chatClient
            .prompt(TrecomPrompt.buildPrompt(TrecomPrompt.LASTNAME_TEXT, lastname, outputConverter))
            .stream()
            .content()
            .doOnSubscribe(subscription -> log.info("Processing lastname: {}", lastname))
            .collectList()
            .map(list -> StringUtils.join(list, ""))
            .mapNotNull(outputConverter::convert)
            .doOnNext(chatResponse -> log.info(
                "Lastname [{}], were processed by AI, the output is: [{}] with reasoning: {}",
                lastname,
                chatResponse.getMessage(),
                chatResponse.getReasoning()
            ))
            .flatMap(chatResponse ->
                "false".equalsIgnoreCase(chatResponse.getMessage())
                    ? Mono.error(new AiProcessingException("Provided word is not a lastname: " + lastname))
                    : Mono.just(chatResponse.getMessage()))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> handleDescription(final String description) {
        return chatClient
            .prompt(TrecomPrompt.buildPrompt(TrecomPrompt.DESCRIPTION_TEXT, description, outputConverter))
            .stream()
            .content()
            .doOnSubscribe(subscription -> log.info("Processing description: {}", description))
            .collectList()
            .map(list -> StringUtils.join(list, ""))
            .mapNotNull(outputConverter::convert)
            .doOnNext(chatResponse ->
                log.info("Description was processed by AI, reasoning: {}", chatResponse.getReasoning()))
            .map(ChatResponse::getMessage)
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> handleNotes(final String notes) {
        return chatClient
            .prompt(TrecomPrompt.buildPrompt(TrecomPrompt.NOTES_TEXT, notes, outputConverter))
            .stream()
            .content()
            .doOnSubscribe(subscription -> log.info("Processing notes: {}", notes))
            .collectList()
            .map(list -> StringUtils.join(list, ""))
            .mapNotNull(outputConverter::convert)
            .doOnNext(chatResponse ->
                log.info("Notes were processed by AI, reasoning: {}", chatResponse.getReasoning()))
            .map(ChatResponse::getMessage)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
