package cloud.cholewa.reporter.config;

import cloud.cholewa.reporter.error.processor.TaskException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskDateResolverTest {

    // 00:30 on 1 October in Warsaw, while UTC (the container clock) still shows 30 September
    private final TaskDateResolver sut = new TaskDateResolver(
        Clock.fixed(Instant.parse("2026-09-30T22:30:00Z"), ZoneId.of("Europe/Warsaw")));

    @Test
    void shouldUseTodayInConfiguredTimeZoneWhenNoDateWasRequested() {
        assertThat(sut.resolve(null)).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void shouldKeepRequestedPastDate() {
        assertThat(sut.resolve(LocalDate.of(2026, 9, 12))).isEqualTo(LocalDate.of(2026, 9, 12));
    }

    @Test
    void shouldAcceptTodayInConfiguredTimeZone() {
        assertThat(sut.resolve(LocalDate.of(2026, 10, 1))).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void shouldRejectFutureDate() {
        assertThatThrownBy(() -> sut.resolve(LocalDate.of(2026, 10, 2)))
            .isInstanceOf(TaskException.class)
            .hasMessage("Task date can not be in the future: 2026-10-02");
    }
}
