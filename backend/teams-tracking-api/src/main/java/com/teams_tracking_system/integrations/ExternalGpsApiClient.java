package com.teams_tracking_system.integrations;

import com.teams_tracking_system.core.config.ExternalApiProperties;
import com.teams_tracking_system.integrations.dtos.ExternalAgentListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalAgentResponse;
import com.teams_tracking_system.integrations.dtos.ExternalCheckInListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalCheckInSyncResponse;
import com.teams_tracking_system.integrations.dtos.ExternalGeofenceListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalLocationListResponse;
import com.teams_tracking_system.integrations.dtos.ExternalRouteResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

@Component
public class ExternalGpsApiClient {

    static final int AGENTS_PAGE_SIZE = 50;
    private static final int MAX_AGENT_PAGES = 1000;

    private final WebClient webClient;
    private final Duration timeout;
    private final int maxAttempts;
    private final Duration backoff;
    private final Sleeper sleeper;
    private final ThreadLocal<ExternalGpsRetryStats> retryStats = ThreadLocal.withInitial(ExternalGpsRetryStats::new);

    @Autowired
    public ExternalGpsApiClient(
            @Qualifier("externalApiWebClient") WebClient webClient,
            ExternalApiProperties externalApiProperties) {
        this(webClient, externalApiProperties, Thread::sleep);
    }

    ExternalGpsApiClient(
            WebClient webClient,
            ExternalApiProperties externalApiProperties,
            Sleeper sleeper) {
        this.webClient = webClient;
        this.timeout = externalApiProperties.getTimeout() != null
                ? externalApiProperties.getTimeout()
                : Duration.ofSeconds(10);
        this.maxAttempts = resolveMaxAttempts(externalApiProperties);
        this.backoff = resolveBackoff(externalApiProperties);
        this.sleeper = sleeper;
    }

    public ExternalAgentListResponse findAgents(Boolean active) {
        resetRetryStats();

        List<ExternalAgentResponse> allAgents = new ArrayList<>();
        int page = 1;

        while (page <= MAX_AGENT_PAGES) {
            ExternalAgentListResponse response = findAgentsPage(active, page, AGENTS_PAGE_SIZE);
            List<ExternalAgentResponse> pageData = response != null && response.data() != null
                    ? response.data()
                    : Collections.emptyList();

            if (pageData.isEmpty()) {
                break;
            }

            allAgents.addAll(pageData);

            if (pageData.size() < AGENTS_PAGE_SIZE) {
                break;
            }

            page++;
        }

        if (page > MAX_AGENT_PAGES) {
            throw new ExternalGpsClientException(
                    "External API request failed.",
                    0,
                    "Agent pagination exceeded " + MAX_AGENT_PAGES + " pages.");
        }

        return new ExternalAgentListResponse(allAgents);
    }

    ExternalAgentListResponse findAgentsPage(Boolean active, int page, int limit) {
        return get(uriBuilder -> {
            UriBuilder builder = uriBuilder.path("/api/v1/agents/");
            if (active != null) {
                builder.queryParam("active", active);
            }
            builder.queryParam("page", page);
            builder.queryParam("limit", limit);
            return builder.build();
        }, ExternalAgentListResponse.class);
    }

    public ExternalAgentResponse findAgentById(String id) {
        resetRetryStats();
        return get(uriBuilder -> uriBuilder.path("/api/v1/agents/{id}").build(id), ExternalAgentResponse.class);
    }

    public ExternalLocationListResponse findLocations(Boolean onlineOnly) {
        resetRetryStats();
        return get(uriBuilder -> {
            UriBuilder builder = uriBuilder.path("/api/v1/locations/");
            if (onlineOnly != null) {
                builder.queryParam("onlineOnly", onlineOnly);
            }
            return builder.build();
        }, ExternalLocationListResponse.class);
    }

    public ExternalCheckInListResponse findCheckIns(String agentId, String type) {
        resetRetryStats();
        return get(uriBuilder -> {
            UriBuilder builder = uriBuilder.path("/api/v1/check-ins/");
            if (agentId != null && !agentId.isBlank()) {
                builder.queryParam("agentId", agentId);
            }
            if (type != null && !type.isBlank()) {
                builder.queryParam("type", type);
            }
            return builder.build();
        }, ExternalCheckInListResponse.class);
    }

    public ExternalCheckInSyncResponse syncCheckIns() {
        resetRetryStats();
        return post(
                uriBuilder -> uriBuilder.path("/api/v1/sync/check-ins").build(),
                ExternalCheckInSyncResponse.class);
    }

    public ExternalGeofenceListResponse findGeofences() {
        resetRetryStats();
        return get(uriBuilder -> uriBuilder.path("/api/v1/geofences/").build(), ExternalGeofenceListResponse.class);
    }

    public ExternalRouteResponse findAgentRoute(String agentId, LocalDate date) {
        resetRetryStats();
        Objects.requireNonNull(date, "date must not be null");

        return get(uriBuilder -> uriBuilder
                .path("/api/v1/agents/{id}/route")
                .queryParam("date", date)
                .build(agentId), ExternalRouteResponse.class);
    }

    private <T> T get(Function<UriBuilder, URI> uriFactory, Class<T> responseType) {
        int attempt = 1;

        while (true) {
            try {
                return getOnce(uriFactory, responseType);
            } catch (ExternalGpsClientException exception) {
                if (!shouldRetry(exception, attempt)) {
                    throw exception;
                }

                retryStats.get().recordRetry(exception.getStatusCode());
                sleepBeforeRetry(exception, attempt);
                attempt++;
            }
        }
    }

    private <T> T post(Function<UriBuilder, URI> uriFactory, Class<T> responseType) {
        int attempt = 1;

        while (true) {
            try {
                return postOnce(uriFactory, responseType);
            } catch (ExternalGpsClientException exception) {
                if (!shouldRetry(exception, attempt)) {
                    throw exception;
                }

                retryStats.get().recordRetry(exception.getStatusCode());
                sleepBeforeRetry(exception, attempt);
                attempt++;
            }
        }
    }

    public ExternalGpsRetryStats getLastRetryStats() {
        return retryStats.get().copy();
    }

    private void resetRetryStats() {
        retryStats.set(new ExternalGpsRetryStats());
    }

    private <T> T getOnce(Function<UriBuilder, URI> uriFactory, Class<T> responseType) {
        return webClient.get()
                .uri(uriFactory)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType)
                .block(timeout);
    }

    private <T> T postOnce(Function<UriBuilder, URI> uriFactory, Class<T> responseType) {
        return webClient.post()
                .uri(uriFactory)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(responseType)
                .block(timeout);
    }

    private Mono<? extends Throwable> mapError(ClientResponse response) {
        String retryAfter = response.headers().asHttpHeaders().getFirst(HttpHeaders.RETRY_AFTER);

        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new ExternalGpsClientException(
                        "External API request failed.",
                        response.statusCode().value(),
                        body,
                        retryAfter));
    }

    private boolean shouldRetry(ExternalGpsClientException exception, int attempt) {
        return attempt < maxAttempts
                && (exception.getStatusCode() == 429 || exception.getStatusCode() == 503);
    }

    private void sleepBeforeRetry(ExternalGpsClientException exception, int attempt) {
        Duration delay = resolveDelay(exception, attempt);
        if (delay.isZero() || delay.isNegative()) {
            return;
        }

        try {
            sleeper.sleep(delay.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new ExternalGpsClientException(
                    "External API retry interrupted.",
                    exception.getStatusCode(),
                    exception.getResponseBody(),
                    exception.getRetryAfter());
        }
    }

    private Duration resolveDelay(ExternalGpsClientException exception, int attempt) {
        if (exception.getStatusCode() == 429) {
            Duration retryAfter = parseRetryAfter(exception.getRetryAfter());
            if (retryAfter != null) {
                return retryAfter;
            }
        }

        return backoff.multipliedBy((long) Math.pow(2, attempt - 1));
    }

    private Duration parseRetryAfter(String retryAfter) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }

        String value = retryAfter.trim();
        try {
            return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            try {
                Duration duration = Duration.between(
                        Instant.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return duration.isNegative() ? Duration.ZERO : duration;
            } catch (RuntimeException exception) {
                return null;
            }
        }
    }

    private int resolveMaxAttempts(ExternalApiProperties externalApiProperties) {
        Integer configuredMaxAttempts = externalApiProperties.getRetry() != null
                ? externalApiProperties.getRetry().getMaxAttempts()
                : null;

        if (configuredMaxAttempts == null || configuredMaxAttempts < 1) {
            return 1;
        }
        return configuredMaxAttempts;
    }

    private Duration resolveBackoff(ExternalApiProperties externalApiProperties) {
        Duration configuredBackoff = externalApiProperties.getRetry() != null
                ? externalApiProperties.getRetry().getBackoff()
                : null;

        if (configuredBackoff == null || configuredBackoff.isNegative()) {
            return Duration.ZERO;
        }
        return configuredBackoff;
    }

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
