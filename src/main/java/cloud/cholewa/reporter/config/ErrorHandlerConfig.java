package cloud.cholewa.reporter.config;

import cloud.cholewa.reporter.error.GlobalErrorWebExceptionHandler;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

@Configuration
public class ErrorHandlerConfig {

    @Bean
    @Order(-2)
    GlobalErrorWebExceptionHandler globalErrorWebExceptionHandler(
        final ErrorAttributes errorAttributes,
        final WebProperties webProperties,
        final ApplicationContext applicationContext,
        final ServerCodecConfigurer serverCodecConfigurer
    ) {
        return new GlobalErrorWebExceptionHandler(
            errorAttributes,
            webProperties.getResources(),
            applicationContext,
            serverCodecConfigurer
        );
    }
}
