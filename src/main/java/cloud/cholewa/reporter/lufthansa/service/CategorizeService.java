package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.lufthansa.model.CategorizationResult;
import cloud.cholewa.reporter.lufthansa.model.Task;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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
        String categoriesWithDescriptions = Arrays.stream(TaskCategory.values())
            .map(category -> String.format("- %s: %s", category.name(), category.getDescription()))
            .collect(Collectors.joining("\n"));

        PromptTemplate promptTemplate = getPromptTemplate();

        Prompt prompt = promptTemplate.create(
            Map.of(
                "categories", categoriesWithDescriptions,
                "description", processedTask.getDescription(),
                "format", outputConverter.getFormat()
            ));

        return Mono.fromCallable(() -> getCategorizationResult(processedTask, prompt))
            .map(result -> {
                processedTask.setCategory(result.getCategory());
                processedTask.setDescription(result.getDescription());
                return processedTask;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private static @NonNull PromptTemplate getPromptTemplate() {
        String userPrompt = """
            Twoim zadaniem jest przypisanie opisu zadania do jednej z dostępnych kategorii oraz uzasadnienie wyboru.
            Nie wymyślaj innych kategorii niż podane.
            Jeżeli opis zadania pasuje do wielu kategorii, zwróć najbardziej pasującą.
            Jeżeli opis zadania nie posiada żadnych istotnych informacji, to tylko w tej sytuacji zwróć kategorię "unknown".
            Jeżeli w opisie zadnia pojawią się błędy z punktu wiedzenia języka polskiego, to je popraw.
            Poprawiony opis zadania ma być logiczny i zgodny z regułami języka polskiego, nie może zawierać błędów gramatycznych, ani ortograficznych.
            
            Dostępne kategorie:
            {categories}
            
            Opis zadania:
            {description}
            
            {format}
            """;

        return new PromptTemplate(userPrompt);
    }

    private CategorizationResult getCategorizationResult(final Task processedTask, final Prompt prompt) {
        String response = chatClient.prompt(prompt).call().content();
        CategorizationResult result = outputConverter.convert(response);

        if (result.getCategory() == TaskCategory.UNKNOWN) {
            log.error("Task was not classified, reasoning: {}", result.getReasoning());
            throw new AiProcessingException("Task could not be classified");
        } else {
            log.info(
                "Task: '{}' was classified as: {} reasoning: {}",
                processedTask.getDescription(), result.getCategory(), result.getReasoning()
            );
            return result;
        }
    }
}
