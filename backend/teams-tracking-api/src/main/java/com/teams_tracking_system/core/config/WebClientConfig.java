package com.teams_tracking_system.core.config;

import io.netty.channel.ChannelOption;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(ExternalApiProperties.class)
public class WebClientConfig {

    @Bean
    @Qualifier("externalApiWebClient")
    public WebClient externalApiWebClient(
            WebClient.Builder webClientBuilder,
            ExternalApiProperties externalApiProperties) {
        String baseUrl = validateBaseUrl(externalApiProperties.getBaseUrl());
        validateApiKey(externalApiProperties.getApiKey());
        Duration timeout = validateTimeout(externalApiProperties.getTimeout());

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout);

        return webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private String validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("external.api.base-url must be configured.");
        }

        String normalizedBaseUrl = baseUrl.trim();
        if (!normalizedBaseUrl.startsWith("http://") && !normalizedBaseUrl.startsWith("https://")) {
            normalizedBaseUrl = "https://" + normalizedBaseUrl;
        }

        URI uri = URI.create(normalizedBaseUrl);
        if (!uri.isAbsolute() || uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalStateException("external.api.base-url must be an absolute URL with scheme and host.");
        }

        return normalizedBaseUrl;
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("external.api.api-key must be configured.");
        }
    }

    private Duration validateTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("external.api.timeout must be greater than zero.");
        }

        return timeout;
    }
}
