package com.example.ShardedSagaWallet.configs;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class ResilienceConfig {

    @Value("${sagaCompensationRetry.max-attempts}")
    private int maxAttempts;

    @Value("${sagaCompensationRetry.wait-duration-ms}")
    private Long waitDurationMs;

    @Bean
    public Retry sagaCompensationRetry() {
        Duration waitDuration = Duration.ofMillis(waitDurationMs);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts) // total attempts including first
                .waitDuration(waitDuration) // wait between retries
                .retryExceptions(RuntimeException.class) // retry only on RuntimeException
                .ignoreExceptions(IllegalArgumentException.class) // ignore IllegalArgument
                .build();

        Retry retry = Retry.of("sagaCompensationRetry", config);

        // Attach event listeners for logging
        retry.getEventPublisher()
                .onRetry(event -> log.info("Retrying {} attempt {}", event.getName(), event.getNumberOfRetryAttempts()))
                .onSuccess(event -> log.info("Retry succeeded for {}", event.getName()))
                .onError(event -> log.error("Retry failed for {}", event.getName()));

        return retry;
    }
}