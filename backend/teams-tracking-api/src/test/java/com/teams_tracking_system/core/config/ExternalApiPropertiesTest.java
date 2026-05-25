package com.teams_tracking_system.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        classes = ExternalApiPropertiesTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExternalApiPropertiesTest {

    private static final Map<String, String> ENV_FILE_VALUES = loadEnvFile();

    @Autowired
    private ExternalApiProperties properties;

    @DynamicPropertySource
    static void externalApiProperties(DynamicPropertyRegistry registry) {
        registry.add("external.api.base-url", () -> firstPresent("EXTERNAL_BASE_URL", "EXTERNAL_URL_BASE"));
        registry.add("external.api.api-key", () -> ENV_FILE_VALUES.get("EXTERNAL_API_KEY"));
        registry.add("external.api.timeout", () -> ENV_FILE_VALUES.get("EXTERNAL_API_TIMEOUT_MS"));
        registry.add("external.api.retry.max-attempts", () -> ENV_FILE_VALUES.get("EXTERNAL_API_RETRY_MAX_ATTEMPTS"));
        registry.add("external.api.retry.backoff", () -> ENV_FILE_VALUES.get("EXTERNAL_API_RETRY_BACKOFF_MS"));
    }

    @Test
    void loadsExternalApiPropertiesFromEnvironment() {
        assertThat(properties.getBaseUrl()).isNotBlank();
        assertThat(properties.getApiKey()).isNotBlank();
        assertThat(properties.getTimeout()).isNotNull().isGreaterThan(Duration.ZERO);
        assertThat(properties.getRetry().getMaxAttempts()).isNotNull().isPositive();
        assertThat(properties.getRetry().getBackoff()).isNotNull().isGreaterThan(Duration.ZERO);
    }

    private static Map<String, String> loadEnvFile() {
        Path envPath = Path.of(System.getProperty("user.dir")).resolve("../../.env").normalize();
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(envPath)) {
                if (line.isBlank() || line.trim().startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                values.put(parts[0].trim(), parts[1].trim());
            }
            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read .env file for ExternalApiPropertiesTest.", exception);
        }
    }

    private static String firstPresent(String firstKey, String secondKey) {
        String value = ENV_FILE_VALUES.get(firstKey);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return ENV_FILE_VALUES.get(secondKey);
    }

    @EnableConfigurationProperties(ExternalApiProperties.class)
    static class TestConfig {
    }
}
