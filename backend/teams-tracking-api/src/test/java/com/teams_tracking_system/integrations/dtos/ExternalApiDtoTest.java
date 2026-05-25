package com.teams_tracking_system.integrations.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class ExternalApiDtoTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deserializesAgentListEnvelope() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "id": "seed_agent_003",
                      "externalId": "ext-agent-003",
                      "name": "Pedro Mendes",
                      "role": "MAINTENANCE",
                      "team": "Alpha",
                      "phone": "+5511999990003",
                      "email": "pedro@tecnico.com",
                      "active": true,
                      "status": "PAUSED",
                      "battery": 30,
                      "lastSeen": "2026-05-22T05:30:00.000Z",
                      "createdAt": "2026-05-23T02:35:34.487Z",
                      "updatedAt": "2026-05-23T02:35:51.093Z"
                    }
                  ]
                }
                """;

        ExternalAgentListResponse response = objectMapper.readValue(json, ExternalAgentListResponse.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).status()).isEqualTo(ExternalAgentStatus.PAUSED);
        assertThat(response.data().get(0).role()).isEqualTo(ExternalAgentRole.MAINTENANCE);
    }

    @Test
    void deserializesLocationListEnvelope() throws Exception {
        String json = """
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
                """;

        ExternalLocationListResponse response = objectMapper.readValue(json, ExternalLocationListResponse.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).latitude()).isEqualByComparingTo(new BigDecimal("-23.5505"));
        assertThat(response.data().get(0).status()).isEqualTo(ExternalAgentStatus.ONLINE);
    }

    @Test
    void deserializesCheckInListEnvelope() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "id": "seed_ci_007",
                      "agentId": "seed_agent_003",
                      "type": "LOW_BATTERY",
                      "source": "EVENT_SYNC",
                      "latitude": -23.5489,
                      "longitude": -46.6388,
                      "address": "Rua Augusta, 200 - Sao Paulo, SP",
                      "accuracy": 20.1,
                      "speed": 0,
                      "notes": "Bateria em 30%",
                      "distanceFromPrevious": 0,
                      "externalEventId": "ext-evt-lowbat-001",
                      "occurredAt": "2026-05-22T05:30:00.000Z",
                      "syncedAt": "2026-05-23T02:35:37.259Z"
                    }
                  ]
                }
                """;

        ExternalCheckInListResponse response = objectMapper.readValue(json, ExternalCheckInListResponse.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).type()).isEqualTo(ExternalCheckInType.LOW_BATTERY);
        assertThat(response.data().get(0).source()).isEqualTo(ExternalSyncSource.EVENT_SYNC);
    }

    @Test
    void deserializesGeofenceListEnvelope() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "id": "seed_geo_003",
                      "externalId": "ext-geo-003",
                      "name": "Deposito Reboucas",
                      "type": "CIRCLE",
                      "coordinatesJson": "{\\"center\\":[-46.652,-23.533],\\"radius\\":500}",
                      "alertOnEnter": false,
                      "alertOnExit": true,
                      "assignedTeams": null,
                      "syncedAt": "2026-05-23T02:35:40.532Z"
                    }
                  ]
                }
                """;

        ExternalGeofenceListResponse response = objectMapper.readValue(json, ExternalGeofenceListResponse.class);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).type()).isEqualTo(ExternalGeofenceType.CIRCLE);
        assertThat(response.data().get(0).assignedTeams()).isNull();
    }
}
