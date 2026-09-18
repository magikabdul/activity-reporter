package cloud.cholewa.reporter.config;

import cloud.cholewa.reporter.error.processor.TaskException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TaskDateResolver {

    private final Clock clock;

    /**
     * @return the requested date, or today (in the configured time zone) when none was given
     * @throws TaskException when the requested date lies in the future
     */
    public LocalDate resolve(@Nullable final LocalDate requested) {
        final LocalDate today = LocalDate.now(clock);

        if (requested == null) {
            return today;
        } else if (requested.isAfter(today)) {
            throw new TaskException("Task date can not be in the future: " + requested);
        }
        return requested;
    }
}
