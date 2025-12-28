package cloud.cholewa.reporter.lufthansa.service;

import cloud.cholewa.reporter.lufthansa.model.TaskCategory;
import cloud.cholewa.reporter.lufthansa.model.TaskEntity;
import cloud.cholewa.reporter.lufthansa.repository.LufthansaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static cloud.cholewa.reporter.lufthansa.model.TaskCategory.ARCHITECTURE_DESIGN;
import static cloud.cholewa.reporter.lufthansa.model.TaskCategory.DOCUMENTATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LufthansaReportServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private LufthansaRepository lufthansaRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    private ChatClient.Builder chatClientBuilder;

    private LufthansaReportService sut;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        sut = new LufthansaReportService(chatClientBuilder, lufthansaRepository);
    }

    @Test
    void shouldReturnEmptyReport_whenNoTasksWereRegistered() {
        when(lufthansaRepository.findAllByByDateAndCategory(any(), any()))
            .thenReturn(Flux.empty());

        sut.getMonthlyReport(2000, 11)
            .as(StepVerifier::create)
            .expectNextCount(0)
            .verifyComplete();

        verify(lufthansaRepository, times(TaskCategory.values().length - 1)).findAllByByDateAndCategory(any(), any());
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void shouldReturnRaportForCategoriesWithTasks() {
        when(lufthansaRepository.findAllByByDateAndCategory(any(), any()))
            .thenReturn(Flux.empty());

        when(lufthansaRepository.findAllByByDateAndCategory(any(), eq(ARCHITECTURE_DESIGN)))
            .thenReturn(Flux.just(TaskEntity.builder().description("Architecture design task").build()));

        when(lufthansaRepository.findAllByByDateAndCategory(any(), eq(DOCUMENTATION)))
            .thenReturn(Flux.just(TaskEntity.builder().description("Documentation task").build()));

        when(chatClient.prompt(any(Prompt.class)).call().content())
            .thenReturn("Summary from AI");

        sut.getMonthlyReport(2000, 11)
            .as(StepVerifier::create)
            .assertNext(reports -> {
                assertThat(reports).hasSize(2);
                assertThat(reports).anySatisfy(report -> assertThat(report.getName()).isEqualTo("DOCUMENTATION"));
                assertThat(reports).anySatisfy(report -> assertThat(report.getName()).isEqualTo("ARCHITECTURE DESIGN"));
            })
            .verifyComplete();

        verify(lufthansaRepository, times(TaskCategory.values().length - 1)).findAllByByDateAndCategory(any(), any());
        verify(chatClient, times(2)).prompt(any(Prompt.class));
    }
}