# API externa - endpoints mapeados

Base URL: `https://desafio-media.onrender.com`

OpenAPI consultado: `GET /docs/json`

Observacao de seguranca: a chave de API nao foi registrada neste arquivo. O OpenAPI consultado nao declarou `securitySchemes`, e os endpoints `GET` testados responderam sem autenticacao. Ainda assim, no backend local a chave deve ser configurada por variavel de ambiente, caso algum endpoint remoto exija autenticacao em outro fluxo.

## Endpoints de consulta

| Metodo | Path | Finalidade | Parametros |
| --- | --- | --- | --- |
| `GET` | `/health` | Verifica disponibilidade da API. | Nenhum |
| `GET` | `/api/v1/agents/` | Lista agentes ordenados por nome. | `active=true|false` opcional |
| `GET` | `/api/v1/agents/{id}` | Busca agente por ID interno. | `id` no path |
| `GET` | `/api/v1/agents/{id}/location` | Busca localizacao atual do agente. | `id` no path |
| `GET` | `/api/v1/agents/{id}/route` | Busca percurso do agente em uma data. | `id` no path, `date=YYYY-MM-DD` obrigatorio |
| `GET` | `/api/v1/locations/` | Lista localizacoes atuais dos agentes ativos. | `onlineOnly=true|false` opcional |
| `GET` | `/api/v1/check-ins/` | Lista check-ins/eventos. | `agentId` opcional, `type` opcional |
| `GET` | `/api/v1/geofences/` | Lista geofences cadastradas. | Nenhum |

## Endpoints de sincronizacao

Esses endpoints sao gatilhos remotos de sincronizacao. Eles executam operacoes no servico da API do desafio; portanto, nao foram disparados durante este mapeamento.

| Metodo | Path | Finalidade | Resposta 200 documentada |
| --- | --- | --- | --- |
| `POST` | `/api/v1/sync/agents` | Sincroniza agentes via upsert por `externalId`; pagina automaticamente com maximo de 50 registros por pagina; trata `429` com `Retry-After` e `503` com backoff exponencial com jitter. | `{ "synced": number }` |
| `POST` | `/api/v1/sync/locations` | Sincroniza localizacoes atuais; descarta leituras com acuracia GPS superior a 50 metros. | `{ "synced": number }` |
| `POST` | `/api/v1/sync/check-ins` | Sincroniza check-ins de forma incremental usando `syncToken` da ultima execucao bem-sucedida. | `{ "synced": number, "syncToken": string }` |
| `POST` | `/api/v1/sync/geofences` | Sincroniza geofences via upsert por `externalId`. | `{ "synced": number }` |
| `POST` | `/api/v1/sync/all` | Executa agentes, localizacoes, check-ins e geofences em paralelo. | `{ "agents": number, "locations": number, "checkIns": number, "geofences": number }` |

## Contratos de dados

### Agente

Campos documentados: `id`, `externalId`, `name`, `role`, `team`, `phone`, `email`, `active`, `status`, `battery`, `lastSeen`, `createdAt`, `updatedAt`.

Enums:

- `role`: `TECHNICIAN`, `MAINTENANCE`, `VENDOR`, `INSTALLER`
- `status`: `ONLINE`, `PAUSED`, `SIGNAL_LOST`, `OFFLINE`

### Localizacao

Campos documentados: `agentId`, `externalId`, `name`, `latitude`, `longitude`, `currentAddress`, `accuracy`, `speed`, `battery`, `status`, `lastSeen`.

Regra documentada: leituras com `accuracy > 50` metros sao descartadas pelo sincronizador.

### Check-in/evento

Campos documentados: `id`, `agentId`, `type`, `source`, `latitude`, `longitude`, `address`, `accuracy`, `speed`, `notes`, `distanceFromPrevious`, `externalEventId`, `occurredAt`, `syncedAt`.

Enums:

- `type`: `CHECKIN`, `CHECKOUT`, `VISIT_COMPLETED`, `STOP_DETECTED`, `STOP_ENDED`, `SIGNAL_LOST`, `SIGNAL_RESTORED`, `LOW_BATTERY`, `GEOFENCE_ENTER`, `GEOFENCE_EXIT`
- `source`: `MANUAL`, `GPS_SYNC`, `EVENT_SYNC`

Observacao: `syncToken` nao faz parte do item de check-in listado por `GET /api/v1/check-ins/`. Ele aparece no contrato do gatilho `POST /api/v1/sync/check-ins` como cursor incremental retornado para a proxima sincronizacao.

### Geofence

Campos documentados: `id`, `externalId`, `name`, `type`, `coordinatesJson`, `alertOnEnter`, `alertOnExit`, `assignedTeams`, `syncedAt`.

Enums:

- `type`: `POLYGON`, `CIRCLE`

### Rota

Campos documentados na resposta: `agentId`, `date`, `points`.

Cada ponto possui: `latitude`, `longitude`, `accuracy`, `timestamp`.

O parametro `date` deve seguir o padrao `YYYY-MM-DD`.

## Verificacoes executadas

- `GET /docs/json`: retornou OpenAPI com 13 operacoes.
- `GET /health`: retornou `200`.
- `GET /api/v1/agents/`: retornou `200`.
- `GET /api/v1/agents/{id}` com `seed_agent_001`: retornou `200`.
- `GET /api/v1/agents/{id}/location` com `seed_agent_001`: retornou `200`.
- `GET /api/v1/agents/{id}/route?date=2026-05-22` com `seed_agent_001`: retornou `500` no momento do teste, apesar de estar documentado no OpenAPI.
- `GET /api/v1/locations/`: retornou `200`.
- `GET /api/v1/check-ins/`: retornou `200`.
- `GET /api/v1/geofences/`: retornou `200`.

## Implicacoes para o backend local

- Implementar client externo usando `WebClient`.
- Tratar a API do desafio como fonte remota, nao como API local.
- Persistir `externalId` e `externalEventId` para idempotencia.
- Para check-ins, armazenar o `syncToken` retornado por `POST /api/v1/sync/check-ins` em `SyncCursor.lastCursorValue`, caso o backend local opte por usar esse gatilho remoto.
- Se o backend local implementar a sincronizacao diretamente por endpoints de listagem, usar `syncedAt`/`occurredAt` e idempotencia por `externalEventId` como estrategia defensiva.
