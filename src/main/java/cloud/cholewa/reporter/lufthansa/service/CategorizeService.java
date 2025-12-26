package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.error.AiProcessingException;
import cloud.cholewa.reporter.lufthansa.model.CategorizationResult;
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

    Mono<TaskCategory> categorize(final String taskDescription) {
        String categoriesWithDescriptions = Arrays.stream(TaskCategory.values())
            .map(category -> String.format("- %s: %s", category.name(), category.getDescription()))
            .collect(Collectors.joining("\n"));

        PromptTemplate promptTemplate = getPromptTemplate();

        Prompt prompt = promptTemplate.create(
            Map.of(
                "categories", categoriesWithDescriptions,
                "description", taskDescription,
                "format", outputConverter.getFormat()
            ));

        return Mono.fromCallable(() -> getTaskCategory(taskDescription, prompt))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private static @NonNull PromptTemplate getPromptTemplate() {
        String userPrompt = """
            Twoim zadaniem jest przypisanie opisu zadania do jednej z dostępnych kategorii oraz uzasadnienie wyboru.
            Nie wymyślaj innych kategorii niż podane.
            Jeżeli nie jest możliwe przypisanie zadania do żadnej z dostępnych kategorii, zwróć kategorię "unknown".
            
            Dostępne kategorie:
            {categories}
            
            Opis zadania:
            {description}
            
            {format}
            """;

        return new PromptTemplate(userPrompt);
    }

    private TaskCategory getTaskCategory(final String taskDescription, final Prompt prompt) {
        String response = chatClient.prompt(prompt).call().content();
        CategorizationResult result = outputConverter.convert(response);

        if (result.getCategory() == TaskCategory.UNKNOWN) {
            log.error("Task was not classified, reasoning: {}", result.getReasoning());
            throw new AiProcessingException("Task could not be classified");
        } else {
            log.info(
                "Task: '{}' was classified as: {} reasoning: {}",
                taskDescription, result.getCategory(), result.getReasoning()
            );
            return result.getCategory();
        }
    }
}
