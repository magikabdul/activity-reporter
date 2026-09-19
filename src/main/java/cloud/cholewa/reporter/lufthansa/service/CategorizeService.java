package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiUnavailableException;
import cloud.cholewa.reporter.lufthansa.model.CategorizationResult;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class CategorizeService {

    private final ChatClient chatClient;
    private final BeanOutputConverter<CategorizationResult> outputConverter;

    public CategorizeService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(CategorizationResult.class);
    }

    Mono<Task> categorize(final Task processedTask) {
        return Mono.fromCallable(() -> getCategorizationResult(processedTask))
            .map(result -> {
                processedTask.setCategory(result.getCategory());
                processedTask.setDescription(result.getDescription());
                processedTask.setReasoning(result.getReasoning());
                return processedTask;
            })
            .onErrorMap(e -> new AiUnavailableException("Failed to categorize the task", e))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * UNKNOWN is not a failure: the task comes back uncategorized, together with the reasoning that tells the user
     * what the description is missing. The category is then picked by hand when the task is completed.
     */
    private CategorizationResult getCategorizationResult(final Task processedTask) {
        String response = chatClient
            .prompt(LufthansaPrompt.buildPrompt(processedTask.getDescription(), outputConverter))
            .call()
            .content();
        CategorizationResult result = outputConverter.convert(response);

        if (result == null || result.getCategory() == null || result.getDescription() == null) {
            throw new IllegalStateException("AI answer is missing the category or the description");
        }

        if (result.getCategory() == TaskCategory.UNKNOWN) {
            log.warn("Task was not classified, reasoning: {}", result.getReasoning());
        } else {
            log.info(
                "Task: '{}' was classified as: {} reasoning: {}",
                processedTask.getDescription(), result.getCategory(), result.getReasoning()
            );
        }
        return result;
    }
}
