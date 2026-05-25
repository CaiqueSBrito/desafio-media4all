package com.teams_tracking_system.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teams_tracking_system.core.config.ExternalApiProperties;
import com.teams_tracking_system.integrations.dtos.ExternalAgentStatus;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ExternalGpsApiClientTest {

    @Test
    void findsAgentsWithOptionalQueryParameter() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        WebClient webClient = buildWebClient(capturedUri, """
                {
                  "data": [
                    {
                      "id": "seed_agent_001",
                      "externalId": "ext-agent-001",
                      "name": "Carlos Silva",
                      "role": "TECHNICIAN",
                      "team": "Alpha",
                      "phone": "+5511999990001",
                      "email": "carlos@tecnico.com",
                      "active": true,
                      "status": "ONLINE",
                      "battery": 85,
                      "lastSeen": "2026-05-22T06:00:00.000Z",
                      "createdAt": "2026-05-23T02:35:33.876Z",
                      "updatedAt": "2026-05-23T02:35:50.470Z"
                    }
                  ]
                }
                """);

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        var response = client.findAgents(true);

        assertThat(capturedUri.get().getPath()).isEqualTo("/api/v1/agents/");
        assertThat(capturedUri.get().getQuery()).isEqualTo("active=true&page=1&limit=50");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).status()).isEqualTo(ExternalAgentStatus.ONLINE);
    }

    @Test
    void findsAgentsAcrossPaginatedResponses() {
        List<URI> capturedUris = new ArrayList<>();
        AtomicInteger requestCount = new AtomicInteger();
        WebClient webClient = buildWebClient(capturedUris, requestCount, List.of(
                agentsResponseJson(50),
                agentsResponseJson(1)));

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        var response = client.findAgents(null);

        assertThat(response.data()).hasSize(51);
        assertThat(capturedUris).hasSize(2);
        assertThat(capturedUris.get(0).getQuery()).isEqualTo("page=1&limit=50");
        assertThat(capturedUris.get(1).getQuery()).isEqualTo("page=2&limit=50");
    }

    @Test
    void findsLocationsWithOptionalOnlineOnlyParameter() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        WebClient webClient = buildWebClient(capturedUri, """
                {
                  "data": [
                    {
                      "agentId": "seed_agent_001",
                      "externalId": "ext-agent-001",
                      "name": "Carlos Silva",
                      "latitude": -23.5505,
                      "longitude": -46.6333,
                      "currentAddress": "Av. Paulista, 1000 - Sao Paulo, SP",
                      "accuracy": 8.5,
                      "speed": 0,
                      "battery": 85,
                      "status": "ONLINE",
                      "lastSeen": "2026-05-22T06:00:00.000Z"
                    }
                  ]
                }
                """);

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        var response = client.findLocations(true);

        assertThat(capturedUri.get().getPath()).isEqualTo("/api/v1/locations/");
        assertThat(capturedUri.get().getQuery()).isEqualTo("onlineOnly=true");
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).agentId()).isEqualTo("seed_agent_001");
    }

    @Test
    void findsAgentRouteWithDateParameter() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        WebClient webClient = buildWebClient(capturedUri, """
                {
                  "agentId": "seed_agent_001",
                  "date": "2026-05-22",
                  "points": [
                    {
                      "latitude": -23.5505,
                      "longitude": -46.6333,
                      "accuracy": 8.5,
                      "timestamp": "2026-05-22T04:00:00.000Z"
                    }
                  ]
                }
                """);

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        var response = client.findAgentRoute("seed_agent_001", LocalDate.of(2026, 5, 22));

        assertThat(capturedUri.get().getPath()).isEqualTo("/api/v1/agents/seed_agent_001/route");
        assertThat(capturedUri.get().getQuery()).isEqualTo("date=2026-05-22");
        assertThat(response.points()).hasSize(1);
    }

    @Test
    void syncCheckInsPostsToIncrementalEndpointAndParsesToken() {
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        AtomicReference<HttpMethod> capturedMethod = new AtomicReference<>();
        WebClient webClient = buildWebClient(capturedUri, capturedMethod, """
                {
                  "synced": 4,
                  "syncToken": "token-after"
                }
                """);

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        var response = client.syncCheckIns();

        assertThat(capturedMethod.get()).isEqualTo(HttpMethod.POST);
        assertThat(capturedUri.get().getPath()).isEqualTo("/api/v1/sync/check-ins");
        assertThat(capturedUri.get().getQuery()).isNull();
        assertThat(response.synced()).isEqualTo(4);
        assertThat(response.syncToken()).isEqualTo("token-after");
    }

    @Test
    void mapsHttpErrorIntoClientException() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"message\":\"not found\"}")
                        .build()))
                .build();

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties());

        assertThatThrownBy(() -> client.findAgentById("missing"))
                .isInstanceOf(ExternalGpsClientException.class)
                .hasMessage("External API request failed.")
                .satisfies(throwable -> {
                    ExternalGpsClientException exception = (ExternalGpsClientException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(404);
                    assertThat(exception.getResponseBody()).contains("not found");
                });
    }

    @Test
    void retriesRateLimitUsingRetryAfterHeader() {
        AtomicInteger requestCount = new AtomicInteger();
        List<Long> sleptMillis = new ArrayList<>();
        WebClient webClient = buildSequentialStatusWebClient(requestCount, List.of(
                ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.RETRY_AFTER, "2")
                        .body("{\"message\":\"rate limited\"}")
                        .build(),
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(agentsResponseJson(1))
                        .build()));

        ExternalGpsApiClient client = new ExternalGpsApiClient(
                webClient,
                externalApiProperties(),
                sleptMillis::add);

        var response = client.findAgents(null);

        assertThat(response.data()).hasSize(1);
        assertThat(requestCount.get()).isEqualTo(2);
        assertThat(sleptMillis).containsExactly(2000L);
        assertThat(client.getLastRetryStats().getRetryAttempts()).isEqualTo(1);
        assertThat(client.getLastRetryStats().getRateLimitErrors()).isEqualTo(1);
        assertThat(client.getLastRetryStats().getServiceUnavailableErrors()).isZero();
    }

    @Test
    void retriesServiceUnavailableUsingConfiguredBackoff() {
        AtomicInteger requestCount = new AtomicInteger();
        List<Long> sleptMillis = new ArrayList<>();
        WebClient webClient = buildSequentialStatusWebClient(requestCount, List.of(
                ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"message\":\"unavailable\"}")
                        .build(),
                ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"message\":\"unavailable\"}")
                        .build(),
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(agentsResponseJson(1))
                        .build()));

        ExternalGpsApiClient client = new ExternalGpsApiClient(
                webClient,
                externalApiProperties(),
                sleptMillis::add);

        var response = client.findAgents(null);

        assertThat(response.data()).hasSize(1);
        assertThat(requestCount.get()).isEqualTo(3);
        assertThat(sleptMillis).containsExactly(1000L, 2000L);
        assertThat(client.getLastRetryStats().getRetryAttempts()).isEqualTo(2);
        assertThat(client.getLastRetryStats().getRateLimitErrors()).isZero();
        assertThat(client.getLastRetryStats().getServiceUnavailableErrors()).isEqualTo(2);
    }

    @Test
    void doesNotRetryNonTransientHttpErrors() {
        AtomicInteger requestCount = new AtomicInteger();
        WebClient webClient = buildSequentialStatusWebClient(requestCount, List.of(
                ClientResponse.create(HttpStatus.BAD_REQUEST)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"message\":\"bad request\"}")
                        .build()));

        ExternalGpsApiClient client = new ExternalGpsApiClient(webClient, externalApiProperties(), millis -> {
        });

        assertThatThrownBy(() -> client.findAgents(null))
                .isInstanceOf(ExternalGpsClientException.class)
                .satisfies(throwable -> assertThat(((ExternalGpsClientException) throwable).getStatusCode())
                        .isEqualTo(400));
        assertThat(requestCount.get()).isEqualTo(1);
    }

    private WebClient buildWebClient(AtomicReference<URI> capturedUri, String body) {
        ExchangeFunction exchangeFunction = request -> {
            capturedUri.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };

        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private WebClient buildWebClient(
            AtomicReference<URI> capturedUri,
            AtomicReference<HttpMethod> capturedMethod,
            String body) {
        ExchangeFunction exchangeFunction = request -> {
            capturedUri.set(request.url());
            capturedMethod.set(request.method());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };

        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private WebClient buildWebClient(
            List<URI> capturedUris,
            AtomicInteger requestCount,
            List<String> responseBodies) {
        ExchangeFunction exchangeFunction = request -> {
            capturedUris.add(request.url());
            int index = requestCount.getAndIncrement();
            String body = responseBodies.get(Math.min(index, responseBodies.size() - 1));

            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        };

        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private WebClient buildSequentialStatusWebClient(
            AtomicInteger requestCount,
            List<ClientResponse> responses) {
        ExchangeFunction exchangeFunction = request -> {
            int index = requestCount.getAndIncrement();
            return Mono.just(responses.get(Math.min(index, responses.size() - 1)));
        };

        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private String agentsResponseJson(int count) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"data\":[");
        for (int index = 1; index <= count; index++) {
            if (index > 1) {
                builder.append(",");
            }
            builder.append(agentJson(index));
        }
        builder.append("]}");
        return builder.toString();
    }

    private String agentJson(int index) {
        return """
                {
                  "id": "seed_agent_%03d",
                  "externalId": "ext-agent-%03d",
                  "name": "Agent %03d",
                  "role": "TECHNICIAN",
                  "team": "Alpha",
                  "phone": "+5511999990001",
                  "email": "agent%03d@example.com",
                  "active": true,
                  "status": "ONLINE",
                  "battery": 85,
                  "lastSeen": "2026-05-22T06:00:00.000Z",
                  "createdAt": "2026-05-23T02:35:33.876Z",
                  "updatedAt": "2026-05-23T02:35:50.470Z"
                }
                """.formatted(index, index, index, index);
    }

    private ExternalApiProperties externalApiProperties() {
        ExternalApiProperties properties = new ExternalApiProperties();
        properties.setBaseUrl("https://desafio-media.onrender.com");
        properties.setApiKey("test-key");
        properties.setTimeout(Duration.ofSeconds(5));

        ExternalApiProperties.Retry retry = new ExternalApiProperties.Retry();
        retry.setMaxAttempts(3);
        retry.setBackoff(Duration.ofSeconds(1));
        properties.setRetry(retry);

        return properties;
    }
}
