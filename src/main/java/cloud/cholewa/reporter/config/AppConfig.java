package cloud.cholewa.reporter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Task dates are calendar days of the user, not of the container (which runs in UTC):
     * a task saved at 00:30 in Warsaw must not land on the previous day (or month).
     */
    @Bean
    Clock clock(@Value("${reporter.time-zone:Europe/Warsaw}") final String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
