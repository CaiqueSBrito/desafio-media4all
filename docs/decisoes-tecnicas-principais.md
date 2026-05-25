# Decisoes tecnicas principais

## Escopo deste documento

Este documento consolida as decisoes tecnicas mais importantes observaveis no estado atual do repositorio em 2026-05-25. Ele descreve o que efetivamente esta implementado ou configurado; quando houver lacuna, isso e apontado explicitamente.

## 1. Separacao clara entre backend operacional e frontend de painel

- O repositorio foi dividido em `backend/teams-tracking-api` e `frontend`.
- O backend centraliza regras de negocio, persistencia e sincronizacao com API externa.
- O frontend foi estruturado como painel operacional desacoplado, consumindo a API interna via HTTP.

Motivacao tecnica:
- Permite evolucao independente entre integracao/sincronizacao e experiencia de operacao.
- Reduz acoplamento entre contratos HTTP e detalhes de persistencia.

## 2. Backend em Spring Boot 3 com WebMVC para API interna e WebClient para integracao externa

- O backend usa `spring-boot-starter-web` para endpoints internos e `spring-boot-starter-webflux` apenas para o cliente HTTP reativo.
- A integracao externa foi encapsulada em `ExternalGpsApiClient`, evitando espalhar chamadas HTTP pelos services.
- O `WebClient` e validado na inicializacao: base URL absoluta, API key obrigatoria e timeout positivo.

Motivacao tecnica:
- Atende a stack obrigatoria do desafio sem misturar controllers com logica de integracao.
- Falhas de configuracao quebram no bootstrap, e nao apenas na primeira sincronizacao em runtime.

## 3. Arquitetura em camadas com regra de negocio concentrada em services

- `controllers`: exposicao HTTP.
- `dtos.requests` e `dtos.schemas`: contratos de entrada e saida.
- `service`: regras de negocio, sincronizacao e monitoramento.
- `repositories`: acesso a banco via Spring Data JPA.
- `model`: entidades JPA.
- `core/config` e `core/exception`: configuracao e tratamento transversal.

Motivacao tecnica:
- Mantem controllers finos.
- Garante que acesso a dados passe por repository e que a orquestracao fique testavel em service.

## 4. Persistencia relacional com Flyway e modelagem voltada a auditoria

- Runtime usa MySQL; testes usam H2 em memoria.
- Evolucao de schema e controlada por migrations SQL versionadas em `db/migration`.
- Alem das tabelas de negocio (`agents`, `agent_positions`, `check_ins`, `geofences`), o projeto persiste telemetria de sincronizacao em `sync_executions`, `sync_cursors`, `sync_failures` e `sync_conflicts`.

Motivacao tecnica:
- O sistema nao trata sincronizacao como efeito colateral invisivel; ele armazena historico, cursor, falhas e conflitos para auditoria e operacao.

## 5. Estrategia explicita de idempotencia e deduplicacao

- `Agent` usa `externalId` como chave de upsert na sincronizacao externa.
- Check-ins manuais aceitam `idempotencyKey`, consultada antes de persistir novo evento.
- Posicoes GPS possuem deduplicacao em memoria por execucao e restricao unica em banco para `(agent_id, last_seen, latitude, longitude)`.
- Reprocessamento de snapshot repetido e tratado como `ignored`, nao como erro.

Motivacao tecnica:
- O projeto assume que reenvio, repeticao de pagina e repeticao de cursor sao cenarios normais de integracao, nao excecoes raras.

## 6. Separacao entre historico de localizacao e estado atual do agente

- Cada evento GPS valido pode virar `AgentPosition` para manter trilha historica.
- O `Agent` tambem guarda um snapshot de localizacao atual (`currentLatitude`, `currentLongitude`, `lastSeen`, etc.).
- Eventos atrasados permanecem no historico, mas nao substituem o estado atual quando forem mais antigos.

Motivacao tecnica:
- Resolve o conflito entre consulta rapida da posicao atual e necessidade de rota/auditoria historica.

## 7. Sincronizacao incremental orientada por cursor e telemetria de execucao

- Cada sync inicia com um registro em `SyncExecution`.
- O ultimo cursor/token valido fica em `SyncCursor`.
- O cursor e atualizado apenas apos execucao considerada bem-sucedida.
- O sistema registra contadores de leitura, criacao, atualizacao, ignorados, falhas, retries, 429 e 503.

Motivacao tecnica:
- Permite retomada controlada, observabilidade e diagnostico sem depender de logs soltos.

## 8. Retry controlado para 429 e 503 no cliente externo

- Retry ocorre somente para `429` e `503`.
- O cliente respeita `Retry-After` quando presente para `429`.
- Quando nao ha `Retry-After`, aplica backoff exponencial sobre a configuracao base.
- Estatisticas de retry sao acumuladas e propagadas para `SyncExecution`.

Motivacao tecnica:
- O projeto diferenciou erro transitorio de erro estrutural, evitando retry cego para qualquer falha HTTP.

## 9. Tratamento de payload invalido e conflito como dado persistido, nao apenas log

- Payload externo invalido gera registro em `SyncFailure`.
- Divergencias relevantes entre estado local e externo geram registro em `SyncConflict`.
- A estrategia privilegia degradacao parcial: o sync pode terminar com `WARNING` sem derrubar toda a execucao.

Motivacao tecnica:
- Em integracao operacional, perder um item problematico e preferivel a interromper todo o ciclo de sincronizacao.

## 10. Controle local de concorrencia por scheduler

- Cada scheduler herda a estrategia de `NonConcurrentSyncScheduler`, baseada em `AtomicBoolean`.
- Uma nova execucao do mesmo job e ignorada se a anterior ainda estiver rodando.

Motivacao tecnica:
- Evita sobreposicao acidental dentro de uma unica instancia da aplicacao.

Limitacao explicita:
- A trava observada e local ao processo. Em ambiente com multiplas replicas, ainda seria necessario lock distribuido.

## 11. Contrato de erro padronizado e validacao centralizada

- Excecoes de dominio derivadas de `ApiException` sao traduzidas por `GlobalExceptionHandler`.
- Falhas de validacao, corpo malformado e parametro invalido recebem resposta HTTP padronizada em `ErrorResponse`.
- `spring.jpa.open-in-view=false` foi configurado para evitar dependencia de sessao aberta fora da camada transacional.

Motivacao tecnica:
- O backend privilegia contratos previsiveis de erro e fronteiras transacionais explicitas.

## 12. Frontend como consumidor tipado da API e cache curto para operacao

- O frontend usa Next.js 16, React 19, TanStack Query, React Hook Form, Zod e shadcn/ui.
- `src/lib/api.ts` centraliza tipos, chamadas HTTP e conversao padrao de erro.
- `QueryProvider` define cache curto (`staleTime` de 30 segundos), sem refetch em foco e retry reduzido.

Motivacao tecnica:
- A UI foi pensada como painel operacional: resposta rapida, pouca duplicacao de cliente HTTP e tratamento uniforme de erro.

## Lacunas ou inconsistencias observadas

- O frontend tem estrutura de componentes e client API, mas `src/app/page.tsx` importa `@/components/dashboard/operational-dashboard`, arquivo que nao esta presente no workspace atual. Isso sugere estado incompleto ou alteracao ainda nao consolidada nessa parte do frontend.
