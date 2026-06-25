# TicketFull API

API REST para autenticação e venda de ingressos, construída com Spring Boot 4, Java 25, PostgreSQL, Flyway e JWT.

Além do módulo de autenticação, o projeto já inclui o domínio de eventos:

- eventos
- lotes de ingressos
- pedidos
- validação de ingressos (check-in)

## Sumário

- Tecnologias
- Funcionalidades
- Papéis e autorização
- Estrutura do projeto
- Pré-requisitos
- Configuração
- Como executar
- Endpoints
- Segurança
- Tratamento de erros
- Testes
- Swagger / OpenAPI
- Docker

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring Security | gerenciado pelo Spring Boot |
| Spring Data JPA | gerenciado pelo Spring Boot |
| Flyway | gerenciado pelo Spring Boot |
| PostgreSQL | 16 (docker-compose) |
| H2 Database (testes) | gerenciado pelo Spring Boot |
| Testcontainers (testes de integração) | gerenciado pelo Spring Boot |
| Actuator + Micrometer/Prometheus | gerenciado pelo Spring Boot |
| JJWT | 0.12.6 |
| Bucket4j | 8.10.1 |
| Springdoc OpenAPI | 3.0.3 |
| Lombok | 1.18.38 |
| JaCoCo | 0.8.13 |
| Maven Wrapper | incluído no repositório |

## Funcionalidades

- Registro, login, refresh e logout com JWT + refresh token com rotação
- Refresh token em cookie HttpOnly (path `/auth`)
- Rate limiting em `/auth/login` por IP (Bucket4j)
- **Ciclo de vida do evento** (`DRAFT`/`PUBLISHED`/`CANCELLED`): atualizar, publicar e cancelar; só eventos publicados aparecem na busca pública
- Lotes de ingressos com **janela de vendas** opcional (`salesStartAt`/`salesEndAt`)
- Compra de ingressos com reserva e prazo (apenas eventos publicados e lotes dentro da janela)
- Confirmação de pagamento via gateway + webhook idempotente; estorno
- **Transferência de ingresso** entre usuários (regenera o código/QR)
- Validação de ingresso por código hash (check-in)
- **Notificação assíncrona** de confirmação de pagamento (evento de domínio + `@TransactionalEventListener` + `@Async`)
- Listagens paginadas (eventos e meus pedidos)
- Migrações versionadas com Flyway
- Respostas de erro padronizadas com Problem Details (RFC 9457)

## Papéis e autorização

Papéis disponíveis no enum `UserRole`:

- `ADMIN`
- `USER`
- `ORGANIZER`
- `CUSTOMER`

Regras de acesso no `SecurityConfig`:

- Público: `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout`, `/webhooks/payments`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `POST /events` e `POST /events/{eventId}/ticket-batches`: `ORGANIZER` ou `ADMIN`
- `POST /orders`, `POST /orders/{id}/checkout`, `POST /orders/{id}/cancel` e `POST /orders/{id}/refund`: `CUSTOMER` ou `ADMIN`
- `POST /tickets/{codeHash}/validate`: `ORGANIZER` ou `ADMIN`
- Demais rotas: autenticadas

## Estrutura do projeto

```text
src/main/java/com/auth/jwt_api/
├── config/                 # OpenAPI, JPA auditing
├── controllers/            # Auth, Events, Ticket Batches, Orders, Tickets
├── dtos/                   # Contratos de entrada/saída da API
├── exceptions/             # Exceções de domínio e handler global
├── models/                 # Entidades e enums (User, Event, Order, Ticket...)
├── repositories/           # Repositórios JPA
├── security/               # JWT filter, rate limit, security handlers/config
└── services/               # Regras de negócio

src/main/resources/db/migration/
├── V1__init_auth_schema.sql
└── V2__create_event_domain.sql
```

## Pré-requisitos

- Java 25+
- Docker e Docker Compose (opcional, mas recomendado)
- PostgreSQL local (se não usar Docker)

## Configuração

Configuração base em `src/main/resources/application.properties`.

### Banco

```sql
CREATE DATABASE jwt_api;
```

### Variáveis de ambiente (segredos)

Os segredos **não** têm valores padrão no código — a aplicação falha no startup se `JWT_SECRET` não for definido. Copie `.env.example` para `.env` e ajuste:

```bash
cp .env.example .env
```

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | sim | Chave de assinatura do JWT (>= 32 caracteres). Ex.: `openssl rand -base64 48` |
| `POSTGRES_PASSWORD` | sim (Docker) | Senha do PostgreSQL |
| `POSTGRES_DB`, `POSTGRES_USER` | não | Padrão `jwt_api` / `postgres` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | não | Se definidas, criam um ADMIN inicial no startup |

### Principais propriedades

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/jwt_api}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.baseline-on-migrate=true

# Segredo obrigatório (sem fallback) — falha no startup se ausente
api.security.token.secret=${JWT_SECRET}
api.security.token.expiration=7200000
api.security.token.refresh-expiration=604800000

# Pool de conexões e limites de requisição
spring.datasource.hikari.maximum-pool-size=10
server.tomcat.max-http-form-post-size=2MB

server.port=8081
```

Para produção, ative o perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`): logs JSON (ECS), Swagger desabilitado, health detalhado apenas para usuários autorizados.

## Como executar

### 1) Subir tudo com Docker Compose (recomendado)

```bash
docker compose up --build -d
```

API disponível em `http://localhost:8081`.

### 2) Rodar local com Maven Wrapper

```bash
./mvnw spring-boot:run
```

Com segredo explícito:

```bash
JWT_SECRET=minha-chave-super-secreta-32chars!! ./mvnw spring-boot:run
```

Build de produção:

```bash
./mvnw clean package -DskipTests
java -jar target/jwt-api-0.0.1-SNAPSHOT.jar
```

## Endpoints

### Autenticação

- `POST /auth/register` (auto-registro público — cria sempre `CUSTOMER`)
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout` (revoga o access token; envie o header `Authorization`)
- `POST /admin/users` (apenas `ADMIN` — provisiona `ORGANIZER`/`ADMIN`)

Exemplo de registro (sempre CUSTOMER):

```json
{
  "email": "cliente@email.com",
  "password": "senha123"
}
```

Para criar um `ORGANIZER`/`ADMIN`, autentique-se como `ADMIN` e use `POST /admin/users` com o campo `role`.

Exemplo de login:

```json
{
  "email": "organizador@email.com",
  "password": "senha123"
}
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200
}
```

### Eventos

- `POST /events` (cria como `DRAFT`)
- `GET /events` (público, paginado; só `PUBLISHED`)
- `GET /events/{id}`
- `PUT /events/{id}` (atualizar — organizador dono)
- `POST /events/{id}/publish` (`DRAFT` -> `PUBLISHED`)
- `POST /events/{id}/cancel` (encerra novas vendas)

Payload de criação/atualização:

```json
{
  "title": "Tech Conference 2026",
  "description": "Evento anual de tecnologia",
  "eventDate": "2026-09-10T19:30:00Z",
  "location": "São Paulo - SP"
}
```

### Lotes de ingressos

- `POST /events/{eventId}/ticket-batches`
- `GET /events/{eventId}/ticket-batches`

Payload de criação (`salesStartAt`/`salesEndAt` opcionais — nulos = sem restrição):

```json
{
  "name": "Lote Promocional",
  "price": 99.90,
  "totalCapacity": 300,
  "salesStartAt": "2026-08-01T00:00:00Z",
  "salesEndAt": "2026-09-09T23:59:59Z"
}
```

### Pedidos

- `POST /orders` (apenas eventos `PUBLISHED` e lotes dentro da janela de vendas)
- `GET /orders` (paginado)
- `GET /orders/{id}`
- `POST /orders/{id}/checkout` (cria PaymentIntent; pagamento confirmado via webhook)
- `POST /orders/{id}/cancel`
- `POST /orders/{id}/refund`
- `POST /webhooks/payments` (callback público e idempotente do gateway)

### Ingressos

- `POST /tickets/{codeHash}/validate` (check-in — organizador dono)
- `POST /tickets/{id}/transfer` (transferir para outro usuário — detentor atual)

Payload de compra:

```json
{
  "ticketBatchId": "8f076dfb-6e0f-49ba-a90c-06c6574d7fdf",
  "quantity": 2
}
```

### Header para rotas protegidas

```http
Authorization: Bearer <access_token>
```

## Segurança

- Senhas com BCrypt
- Sessão stateless (`SessionCreationPolicy.STATELESS`)
- JWT assinado para access token (com `jti` para permitir revogação)
- **Revogação de access token no logout** (lista de bloqueio em banco, validada pelo filtro de segurança; limpeza periódica das entradas expiradas)
- Refresh token persistido com expiração e rotação
- Cookie de refresh com `HttpOnly`, `Secure`, `SameSite=Lax`, path `/auth`
- **Auto-registro restrito a `CUSTOMER`**; papéis privilegiados só por `ADMIN` (`/admin/users`)
- **Segredos sem fallback no código** — `JWT_SECRET` obrigatório no startup
- CORS com credenciais habilitadas
- CSRF desabilitado para API stateless
- Rate limit no login com resposta `429` e `Retry-After`

Nota importante para desenvolvimento local: como o cookie de refresh é `Secure`, navegadores só o enviam em HTTPS. Para validar o ciclo completo de refresh/logout em ambiente local, prefira cliente HTTP que suporte esse fluxo (Postman/curl) ou ajuste de ambiente específico para desenvolvimento.

## Tratamento de erros

A API usa Problem Details (RFC 9457).

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Credenciais inválidas"
}
```

Status comuns:

- `400 Bad Request`: validação de payload
- `401 Unauthorized`: credenciais ou token inválido/expirado
- `403 Forbidden`: sem permissão
- `404 Not Found`: recurso inexistente
- `409 Conflict`: conflito de negócio (ex.: e-mail já cadastrado)
- `429 Too Many Requests`: limite de tentativas de login excedido
- `500 Internal Server Error`: erro inesperado

## Testes

- **Testes unitários e de slice** usam H2 em memória (`src/test/resources/application-test.properties`), com Flyway desabilitado no perfil `test`.
- **Teste de integração ponta-a-ponta** (`OrderFlowIntegrationTest`) sobe um **PostgreSQL real via Testcontainers** (requer Docker), roda as migrações Flyway V1–V3, valida o mapeamento JPA (`ddl-auto=validate`) e exercita os locks pessimistas e todo o ciclo de vida do pedido.

Executar testes:

```bash
./mvnw test
```

Executar a verificação completa (testes + relatório e gate de cobertura JaCoCo em `target/site/jacoco/`):

```bash
./mvnw verify
```

## Observabilidade

Com a aplicação em execução, o Actuator expõe:

- `GET /actuator/health` — health check (com liveness/readiness probes)
- `GET /actuator/info` — metadados da aplicação
- `GET /actuator/prometheus` — métricas no formato Prometheus
- `GET /actuator/metrics` — métricas via API do Actuator

Métricas de negócio customizadas (Micrometer):

- `ticketfull_tickets_sold_total` — ingressos vendidos
- `ticketfull_orders_total{status=PENDING|PAID|CANCELLED|EXPIRED|REFUNDED}` — transições de pedido

O `docker-compose.yml` sobe **Prometheus** (http://localhost:9090) e **Grafana** (http://localhost:3000, datasource já provisionado) para visualizar as métricas. Cada requisição recebe um `X-Correlation-Id` (gerado ou reaproveitado do header), propagado para os logs via MDC.

## CI/CD

`.github/workflows/ci.yml` (GitHub Actions):

- **build**: `./mvnw verify` (testes + Testcontainers + gate de cobertura) em JDK 25, com upload do relatório JaCoCo como artefato.
- **docker**: em push na `main`, faz build da imagem e publica em `ghcr.io` (autenticação via `GITHUB_TOKEN`).

## Swagger / OpenAPI

Com a aplicação em execução:

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

Para endpoints protegidos, use o botão Authorize e informe o access token.

## Docker

Arquivos do projeto:

- `Dockerfile`: build multi-stage (Maven + JRE 25)
- `docker-compose.yml`: sobe API + PostgreSQL 16 + Prometheus + Grafana
- `monitoring/`: configuração do Prometheus e provisionamento do datasource do Grafana

Comando:

```bash
docker compose up --build -d
```

Parar serviços:

```bash
docker compose down
```

