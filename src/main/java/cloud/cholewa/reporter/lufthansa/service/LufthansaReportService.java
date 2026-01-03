package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.ReportResponse;
import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LufthansaReportService {

    private final LufthansaRepository lufthansaRepository;
    private final ChatClient chatClient;

    public LufthansaReportService(
        ChatClient.Builder chatClientBuilder,
        LufthansaRepository lufthansaRepository
    ) {
        this.lufthansaRepository = lufthansaRepository;
        this.chatClient = chatClientBuilder.build();
    }

    public Mono<List<ReportResponse>> getMonthlyReport(final int year, final int month) {
        return Flux.fromArray(TaskCategory.values())
            .filter(category -> category != TaskCategory.UNKNOWN)
            .doOnNext(category -> log.info("Generating report for category: {}", category))
            .flatMap(taskCategory -> getCategorySummary(LocalDate.of(year, month, 1), taskCategory))
            .doOnNext(reportResponse -> {
                if (reportResponse.getSummary().equals("N/A")) {
                    log.info(
                        "No report data found for year {} and month {} for category {}",
                        year,
                        month,
                        reportResponse.getName()
                    );
                }
            })
            .filter(reportResponse -> !reportResponse.getSummary().equals("N/A"))
            .collectList()
            .filter(reports -> !reports.isEmpty());
    }

    private Mono<ReportResponse> getCategorySummary(final LocalDate date, final TaskCategory category) {
        return lufthansaRepository.findAllByByDateAndCategory(date, category)
            .map(TaskEntity::getDescription)
            .collectList()
            .filter(descriptions -> !descriptions.isEmpty())
            .map(descriptions -> String.join(", ", descriptions))
            .doOnNext(description -> log.info("Processing synthesize for category {}", category))
            .flatMap(description -> synthesizeReport(description)
                .map(summary -> ReportResponse.builder()
                    .name(category.name().replace("_", " "))
                    .description(category.getDescription())
                    .summary(summary)
                    .build()
                )
            )
            .onErrorResume(e -> {
                log.warn("Failed to generate AI summary for category {}, using original description instead", category);
                return Mono.empty();
            });
    }

    private static PromptTemplate getPromptTemplate() {
        String synthesizePrompt = """
            1. Na bazie listy podanych opisów zadań przygotuj podsumowanie tylko jako jeden opis.
            2. Jeżeli nie zostały podane żadne opisy, zwróć `N/A`
            3. Opis nie może być większy niż 500 znaków.
            4. Opis musi być w języku polskim.
            5. Nie używaj słowa przeglądy, tylko recenzja, jeżeli opisujesz zadania związane z recenzowaniem.
            
            Opisy zadań:
            {descriptions}
            """;

        return new PromptTemplate(synthesizePrompt);
    }

    private Mono<String> synthesizeReport(final String descriptions) {
        return Mono.fromCallable(() ->
                chatClient.prompt(getPromptTemplate().create(Map.of("descriptions", descriptions)))
                    .call().content())
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(error ->
                log.error("Error during API call to OpenAI: {}", error.getMessage()));
    }
}
