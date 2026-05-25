# Teams Tracking System

Sistema fullstack para gestao e rastreamento de agentes de campo, com sincronizacao de dados de uma API GPS externa, historico de posicoes, check-ins manuais, rota diaria e monitoramento operacional da integracao.

## Estado Atual

Implementado:

- Backend Spring Boot com CRUD de agentes, check-ins, historico de rota, sincronizacao externa e monitoramento.
- Frontend Next.js com painel operacional, listagem/filtro de agentes, formularios, detalhes, posicao atual, check-ins e rota do dia.
- Integracao com API externa via `WebClient`.
- Quatro schedulers independentes: agentes, localizacoes, check-ins e geofences.
- Persistencia de historico de sincronizacao, cursores, falhas e conflitos.
- Tratamento de paginacao, `syncToken`, retry/backoff, 429 e 503.
- Swagger/OpenAPI no backend.
- Testes automatizados relevantes no backend.

Nao implementado nesta versao:

- Dockerizacao completa.
- Circuit Breaker com Resilience4j.
- Mapa interativo com Leaflet.
- WebSocket/SSE.
- Geofencing visual no frontend.

## Stack

Backend:

- Java 17+
- Spring Boot 3.5
- Spring MVC
- Spring Data JPA
- MySQL
- Flyway
- WebClient
- Spring Validation
- springdoc OpenAPI
- H2 para testes

Frontend:

- Next.js 16
- React 19
- TypeScript
- Tailwind CSS 4
- TanStack Query
- React Hook Form
- Zod
- shadcn/ui/Radix UI

## Estrutura

```text
projeto_react/
|-- backend/
|   `-- teams-tracking-api/
|       |-- pom.xml
|       `-- src/
|           |-- main/
|           |   |-- java/com/teams_tracking_system/
|           |   |   |-- controllers/
|           |   |   |-- core/
|           |   |   |-- dtos/
|           |   |   |-- integrations/
|           |   |   |-- model/
|           |   |   |-- repositories/
|           |   |   |-- schedulers/
|           |   |   `-- service/
|           |   `-- resources/
|           |       |-- application.properties
|           |       |-- application-dev.properties
|           |       |-- application-prod.properties
|           |       |-- application-test.properties
|           |       `-- db/migration/
|           `-- test/
|-- frontend/
|   |-- package.json
|   |-- next.config.ts
|   `-- src/
|       |-- app/
|       |-- components/
|       |-- lib/
|       `-- providers/
|-- docs/
|-- .todo
`-- README.md
```

Observacao: o `docker-compose.yml` nao existe no estado atual do workspace.

## Arquitetura

O backend segue separacao de responsabilidades:

- `controllers`: endpoints HTTP.
- `dtos/requests`: contratos de entrada.
- `dtos/schemas`: contratos de saida.
- `service`: regras de negocio e orquestracao.
- `repositories`: persistencia e consultas.
- `model`: entidades JPA e enums de dominio.
- `core/config`: configuracoes compartilhadas.
- `core/exception`: tratamento global de erros.
- `integrations`: cliente da API GPS externa usando `WebClient`.
- `schedulers`: jobs independentes de sincronizacao.

O frontend centraliza chamadas HTTP em `frontend/src/lib/api.ts`, usa TanStack Query para cache/refetch e organiza a experiencia principal em `frontend/src/components/dashboard`.

## Variaveis de Ambiente

O backend importa variaveis de ambiente do sistema e tambem tenta carregar `.env` na raiz do repositorio via `spring.config.import`.

Backend:

```env
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:mysql://localhost:3306/teams_tracking_system?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=sua-senha-local

EXTERNAL_BASE_URL=https://host-da-api-externa
EXTERNAL_API_KEY=sua-chave-da-api
EXTERNAL_API_TIMEOUT_MS=10000
EXTERNAL_API_RETRY_MAX_ATTEMPTS=3
EXTERNAL_API_RETRY_BACKOFF_MS=20000

SYNC_SCHEDULERS_ENABLED=true
SYNC_SCHEDULER_AGENTS_INITIAL_DELAY_MS=30000
SYNC_SCHEDULER_AGENTS_FIXED_DELAY_MS=300000
SYNC_SCHEDULER_LOCATIONS_INITIAL_DELAY_MS=30000
SYNC_SCHEDULER_LOCATIONS_FIXED_DELAY_MS=60000
SYNC_SCHEDULER_CHECK_INS_INITIAL_DELAY_MS=30000
SYNC_SCHEDULER_CHECK_INS_FIXED_DELAY_MS=60000
SYNC_SCHEDULER_GEOFENCES_INITIAL_DELAY_MS=30000
SYNC_SCHEDULER_GEOFENCES_FIXED_DELAY_MS=600000
SYNC_LOCATIONS_MAX_ACCURACY_METERS=50
```

Aliases aceitos para URL/chave externa:

- URL: `EXTERNAL_BASE_URL`, `EXTERNAL_URL_BASE`, `EXTERNAL_API_BASE_URL`, `URL_BASE`.
- Chave: `EXTERNAL_API_KEY`, `API_KEY`.

Frontend:

```env
BACKEND_API_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_PATH=/api/backend
```

Nao publique chaves reais no repositorio.

## Execucao Local

### 1. Banco de dados

Crie um banco MySQL acessivel pela `DB_URL`. O profile `dev` usa MySQL e valida o schema com Flyway/JPA.

Exemplo de database:

```sql
CREATE DATABASE IF NOT EXISTS teams_tracking_system;
```

### 2. Backend

```powershell
Set-Location .\backend\teams-tracking-api
.\mvnw.cmd spring-boot:run
```

Comportamento esperado:

- Porta padrao: `8080`.
- Profile padrao: `dev`.
- Migrations Flyway aplicadas automaticamente.
- Schedulers habilitados por padrao, salvo `SYNC_SCHEDULERS_ENABLED=false`.

Swagger/OpenAPI:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/v3/api-docs`

### 3. Frontend

```powershell
Set-Location .\frontend
npm install
npm run dev
```

Comportamento esperado:

- Porta padrao: `3000`.
- O Next.js redireciona `/api/backend/:path*` para `BACKEND_API_URL`.
- A tela principal fica em `http://localhost:3000`.

## Endpoints Internos

Agentes:

- `GET /api/v1/agents`
- `POST /api/v1/agents`
- `GET /api/v1/agents/{id}`
- `PUT /api/v1/agents/{id}`
- `DELETE /api/v1/agents/{id}`

Check-ins:

- `GET /api/v1/agents/{agentId}/check-ins`
- `POST /api/v1/agents/{agentId}/check-ins`

Rotas:

- `GET /api/v1/agents/{id}/route?date=YYYY-MM-DD`

Sincronizacao manual:

- `POST /api/v1/sync/agents`
- `POST /api/v1/sync/locations`
- `POST /api/v1/sync/check-ins`
- `POST /api/v1/sync/geofences`

Monitoramento:

- `GET /api/v1/monitoring/sync`

## Funcionalidades da Interface

O painel frontend exibe:

- Indicadores de agentes ativos, agentes sem posicao recente, falhas de sync, retries, erros 429/503 e check-ins recentes.
- Tabela de agentes com busca, filtro por status e acoes rapidas.
- Criacao e edicao de agentes.
- Registro de check-in manual.
- Detalhes do agente com resumo, posicao atual, check-ins, rota e historico.
- Rota do dia com distancia total, primeiro evento, ultimo evento e pontos ordenados.
- Monitoramento tecnico com ultima execucao, falhas, retries, HTTP 429/503 e cursor.

Os badges visuais de status `Falha` e `Sucesso` foram removidos do header e dos cards de monitoramento por decisao de apresentacao.

## Sincronizacao

Schedulers implementados:

- `AgentSyncScheduler`: sincroniza agentes externos.
- `LocationSyncScheduler`: sincroniza localizacoes e atualiza posicao atual/historico.
- `CheckInSyncScheduler`: sincroniza check-ins usando cursor/token incremental.
- `GeofenceSyncScheduler`: sincroniza geofences externas.

Cada execucao registra:

- inicio e fim;
- status persistido;
- registros lidos, criados, atualizados, ignorados e com erro;
- tentativas de retry;
- ocorrencias 429 e 503;
- cursor antes/depois;
- mensagem de erro quando aplicavel.

## Paginacao, Retry e Cursor

Paginacao:

- Agentes externos sao buscados em paginas de ate `50` registros.
- A sincronizacao segue ate pagina vazia ou menor que o tamanho esperado.
- Ha limite defensivo de paginas para evitar loop infinito.

Retry:

- `429`: usa `Retry-After` quando disponivel; caso contrario usa backoff.
- `503`: usa backoff.
- Outros erros HTTP nao entram em retry automatico.

Cursores:

- `AGENTS`: cursor por checkpoint/pagina processada.
- `LOCATIONS`: cursor pelo maior `lastSeen` processado.
- `CHECK_INS`: cursor por `syncToken` retornado pela API externa.
- `GEOFENCES`: cursor pelo maior `syncedAt` recebido.

## Idempotencia e Conflitos

Estrategias implementadas:

- Agentes externos usam `externalId` para upsert.
- Check-ins manuais aceitam `idempotencyKey`.
- Posicoes GPS repetidas sao deduplicadas por agente, timestamp e coordenadas.
- Reprocessar pagina/cursor nao deve duplicar dados persistidos.
- Payloads invalidos viram registros em `SyncFailure`.
- Conflitos relevantes entre dado local e externo viram registros em `SyncConflict`.

## Testes e Validacao

Backend:

```powershell
Set-Location .\backend\teams-tracking-api
.\mvnw.cmd test
```

Frontend:

```powershell
Set-Location .\frontend
npm run build
```

Observacao: o script `npm run lint` ainda aponta para `next lint`, comando que nao esta disponivel na CLI do Next.js 16 usada neste projeto.

## Diferenciais Implementados

- Swagger/OpenAPI.
- Testes automatizados no backend.
- Painel de monitoramento operacional.
- Registro de falhas de sincronizacao.
- Registro de conflitos de dados.
- Historico completo de sincronizacoes.

## Limitacoes Conhecidas

- Dockerizacao completa ainda nao foi implementada.
- O README documenta execucao local, nao execucao com Docker.
- Nao ha mapa Leaflet embutido; a interface usa mini mapa visual e link para abrir coordenadas no Google Maps.
- Nao ha WebSocket/SSE para atualizacao em tempo real.
- Nao ha Circuit Breaker com Resilience4j.
- Nao ha testes automatizados de formulario no frontend.
- A chave real da API externa deve ser configurada localmente e nao deve ser versionada.

## Documentacao Complementar

- [docs/enunciado.md](docs/enunciado.md)
- [docs/api-externa-endpoints.md](docs/api-externa-endpoints.md)
- [docs/decisoes-tecnicas-principais.md](docs/decisoes-tecnicas-principais.md)
