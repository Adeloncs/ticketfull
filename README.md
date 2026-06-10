# JWT Auth API

API REST de autenticação stateless construída com **Spring Boot 4**, **Java 25** e **JWT (JSON Web Tokens)**. Fornece registro de usuários, login, renovação de tokens via *refresh token* com rotação automática e documentação interativa via Swagger UI.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Arquitetura do Projeto](#arquitetura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Fluxo de Autenticação](#fluxo-de-autenticação)
- [Segurança](#segurança)
- [Tratamento de Erros](#tratamento-de-erros)
- [Testes](#testes)
- [Documentação Interativa (Swagger)](#documentação-interativa-swagger)
- [Docker](#docker)

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring Security | 6.x |
| jjwt (JJWT) | 0.12.6 |
| Spring Data JPA | — |
| PostgreSQL | — |
| H2 Database (testes) | — |
| Springdoc OpenAPI (Swagger) | 3.0.3 |
| Bucket4j | 8.10.1 |
| Lombok | 1.18.38 |
| Maven | — |

---

## Funcionalidades

- **Registro de usuário** com e-mail, senha (BCrypt) e papel (`ADMIN` ou `USER`)
- **Login** com geração de *access token* (JWT) e *refresh token*
- **Renovação de token** via *refresh token* com rotação automática (o token antigo é invalidado e um novo é emitido)
- **Logout** com invalidação do refresh token e limpeza do cookie
- **Refresh token via cookie HttpOnly** — protegido contra XSS; nunca exposto ao JavaScript
- **Autenticação stateless** — sem sessão no servidor, baseada exclusivamente em JWT
- **Rate limiting** no endpoint de login — limite de tentativas por IP via Bucket4j, com resposta `429 Too Many Requests` e header `Retry-After`
- **CORS configurável** para integração com frontends
- **Controle de acesso por papéis** — `ROLE_USER` e `ROLE_ADMIN`
- **Auditoria automática** de criação e última modificação nos registros de usuário (`createdAt`, `updatedAt`)
- **Tratamento global de erros** padronizado no formato RFC 9457 (*Problem Details*)
- **Documentação interativa** via Swagger UI / OpenAPI 3

---

## Arquitetura do Projeto

```
src/main/java/com/auth/jwt_api/
├── config/
│   ├── JpaAuditingConfig.java        # Habilita auditoria JPA
│   └── OpenApiConfig.java            # Configuração do Swagger/OpenAPI
├── controllers/
│   └── AuthController.java           # Endpoints: /auth/login, /auth/register, /auth/refresh, /auth/logout
├── dtos/
│   ├── AuthenticationRequestDTO.java # Payload de login (email + senha)
│   ├── LoginResponseDTO.java         # Resposta com accessToken + tokenType + expiresIn
│   └── RegisterRequestDTO.java       # Payload de registro (email + senha + role)
├── exceptions/
│   ├── BusinessException.java        # Exceção base de negócio
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice — tratamento centralizado
│   ├── InvalidCredentialsException.java
│   ├── InvalidRefreshTokenException.java
│   ├── TooManyRequestsException.java  # Exceção de rate limit (429)
│   └── UserAlreadyExistsException.java
├── models/
│   ├── RefreshToken.java             # Entidade de refresh token (UUID, expiração, usuário)
│   ├── User.java                     # Entidade de usuário (implementa UserDetails)
│   └── UserRole.java                 # Enum: ADMIN | USER
├── repositories/
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── security/
│   ├── CustomAccessDeniedHandler.java       # 403 Forbidden personalizado
│   ├── CustomAuthenticationEntryPoint.java  # 401 Unauthorized personalizado
│   ├── RateLimiterService.java              # Buckets por IP (Bucket4j)
│   ├── RateLimitingFilter.java             # Filtro de rate limit em /auth/login
│   ├── SecurityConfig.java                  # Configuração do Spring Security
│   ├── SecurityFilter.java                  # Filtro JWT (OncePerRequestFilter)
│   └── TokenService.java                    # Geração e validação de JWT
└── services/
    ├── AuthorizationService.java    # Implementa UserDetailsService
    ├── AuthService.java             # Lógica de login e registro
    └── RefreshTokenService.java     # Criação e rotação de refresh tokens
```

---

## Pré-requisitos

- **Java 25+** — recomendado via [SDKMAN](https://sdkman.io/)
- **Maven 3.9+** (ou usar o wrapper `./mvnw` incluso)
- **PostgreSQL** em execução local (porta `5432`)

---

## Configuração

### Banco de dados

Crie o banco de dados no PostgreSQL antes de iniciar:

```sql
CREATE DATABASE jwt_api;
```

### Variáveis de ambiente / `application.properties`

O arquivo `src/main/resources/application.properties` contém as configurações padrão:

```properties
# Banco de dados
spring.datasource.url=jdbc:postgresql://localhost:5432/jwt_api
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT
api.security.token.secret=${JWT_SECRET:my-secret-key-for-dev-only-32chars!!}
api.security.token.expiration=7200000          # 2 horas (em ms)
api.security.token.refresh-expiration=604800000 # 7 dias (em ms)

# CORS (separar múltiplas origens por vírgula)
app.cors.allowed-origins=http://localhost:3000

# Rate Limiting (endpoint /auth/login)
rate-limit.capacity=5          # máximo de requisições por janela
rate-limit.refill-minutes=5    # janela de recarga (em minutos)

server.port=8081
```

> **Importante:** Em produção, defina a variável de ambiente `JWT_SECRET` com uma chave segura de pelo menos 32 caracteres. Nunca comite segredos reais no repositório.

---

## Como Executar

### Usando o wrapper Maven

```bash
./mvnw spring-boot:run
```

### Passando o segredo JWT via variável de ambiente

```bash
JWT_SECRET=minha-chave-super-secreta-32chars!! ./mvnw spring-boot:run
```

### Gerando o JAR e executando

```bash
./mvnw clean package -DskipTests
java -jar target/jwt-api-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em `http://localhost:8081`.

---

## Endpoints da API

Todos os endpoints abaixo estão sob o prefixo `/auth`.

### `POST /auth/register` — Registrar usuário

**Corpo da requisição:**
```json
{
  "email": "usuario@email.com",
  "password": "senha123",
  "role": "USER"
}
```

> O campo `role` aceita `USER` ou `ADMIN`.

**Resposta de sucesso:** `201 Created` (sem corpo)

---

### `POST /auth/login` — Autenticar usuário

**Corpo da requisição:**
```json
{
  "email": "usuario@email.com",
  "password": "senha123"
}
```

**Resposta de sucesso:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200
}
```

> O *refresh token* é enviado automaticamente como cookie `HttpOnly` (não aparece no corpo da resposta). O cookie possui as flags `HttpOnly`, `Secure`, `SameSite=Lax` e path `/auth`.

---

### `POST /auth/refresh` — Renovar access token

> O refresh token é lido automaticamente do cookie `HttpOnly` definido no login. Não é necessário enviar corpo na requisição.

**Resposta de sucesso:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200
}
```

> O refresh token anterior é invalidado imediatamente (rotação automática). Um novo cookie `HttpOnly` é definido na resposta.

---

### `POST /auth/logout` — Encerrar sessão

> Lê o cookie `refreshToken` (se presente), invalida o token no banco e apaga o cookie da resposta.

**Resposta de sucesso:** `204 No Content`

---

### Endpoints protegidos

Para acessar qualquer endpoint que exija autenticação, envie o *access token* no cabeçalho:

```
Authorization: Bearer <access_token>
```

> O cookie de refresh token é gerenciado automaticamente pelo navegador ou cliente HTTP (ex.: `credentials: 'include'` em `fetch`). Não é necessário manipulá-lo manualmente.

---

## Fluxo de Autenticação

```
Cliente                              Servidor
  │                                     │
  │── POST /auth/login ────────────────►│
  │                                     │  Valida credenciais (BCrypt)
  │                                     │  Gera accessToken (JWT, 2h)
  │                                     │  Gera refreshToken (UUID, 7d)
  │◄── 200 { accessToken, tokenType,    │
  │         expiresIn }                 │
  │    Set-Cookie: refreshToken=...     │  Cookie HttpOnly, Secure, SameSite=Lax
  │                                     │
  │── GET /recurso-protegido ──────────►│
  │   Authorization: Bearer JWT         │  Filtra JWT → extrai e-mail
  │                                     │  Carrega usuário → autentica contexto
  │◄── 200 OK ─────────────────────────│
  │                                     │
  │  (access token expirado)            │
  │── POST /auth/refresh ──────────────►│
  │   Cookie: refreshToken=<uuid>       │  Valida refresh token + expiração
  │                                     │  Gera novo accessToken + novo refreshToken
  │                                     │  Invalida refresh token antigo
  │◄── 200 { accessToken, tokenType,    │
  │         expiresIn }                 │
  │    Set-Cookie: refreshToken=novo    │
  │                                     │
  │── POST /auth/logout ───────────────►│
  │   Cookie: refreshToken=<uuid>       │  Invalida refresh token no banco
  │◄── 204 No Content                   │
  │    Set-Cookie: refreshToken=;Max-Age=0│
```

---

## Segurança

- **Senhas** armazenadas com hash **BCrypt**
- **Access token** JWT assinado com HMAC-SHA256, expiração configurável (padrão: 2 horas)
- **Refresh token** armazenado no banco de dados com expiração configurável (padrão: 7 dias); utiliza estratégia de **rotação** — a cada uso um novo token é gerado e o anterior é deletado
- **Refresh token via cookie HttpOnly** — nunca exposto ao JavaScript; flags `Secure`, `SameSite=Lax`, path restrito a `/auth`
- **Sessão stateless** — Spring Security configurado com `SessionCreationPolicy.STATELESS`
- **Rate limiting** no endpoint de login — máximo de 5 tentativas por IP em janelas de 5 minutos (configurável); retorna `429 Too Many Requests` com header `Retry-After` ao exceder o limite
- **CSRF** desabilitado (adequado para APIs REST stateless com cookie SameSite)
- **CORS** configurável via `app.cors.allowed-origins`; `allowCredentials=true` para suportar envio de cookies
- Rotas públicas: `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout`, `/v3/api-docs/**`, `/swagger-ui/**`
- Todas as demais rotas exigem autenticação

---

## Tratamento de Erros

A API utiliza o padrão [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457) para respostas de erro.

**Exemplo de resposta de erro:**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Credenciais inválidas"
}
```

| Situação | Status HTTP |
|---|---|
| Credenciais inválidas | `401 Unauthorized` |
| Refresh token inválido ou expirado | `401 Unauthorized` |
| E-mail já cadastrado | `409 Conflict` |
| Campos obrigatórios ausentes/inválidos | `400 Bad Request` |
| Recurso sem permissão de acesso | `403 Forbidden` |
| Muitas tentativas de login (rate limit) | `429 Too Many Requests` |
| Erro interno inesperado | `500 Internal Server Error` |

---

## Testes

Os testes utilizam banco de dados **H2 em memória** e são configurados pelo perfil `test` (`src/test/resources/application-test.properties`).

| Arquivo de teste | Cobertura |
|---|---|
| `AuthControllerTest` | Endpoints de login, registro, refresh e logout |
| `AuthServiceTest` | Lógica de autenticação e registro |
| `TokenServiceTest` | Geração e validação de JWT |
| `RateLimiterServiceTest` | Lógica de consumo de tokens (Bucket4j) |
| `RateLimitingIntegrationTest` | Comportamento do filtro de rate limit em `/auth/login` |

```bash
# Executar todos os testes
./mvnw test

# Executar com relatório de cobertura (se configurado)
./mvnw verify
```

---

## Documentação Interativa (Swagger)

Com a aplicação em execução, acesse:

- **Swagger UI:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

Para testar endpoints protegidos no Swagger UI, clique em **Authorize** e informe o *access token* obtido no login.

> **Nota:** O fluxo de cookie HttpOnly não funciona diretamente pelo Swagger UI. Use uma ferramenta como Postman ou curl para testar o ciclo completo de refresh/logout.

---

## Docker

A aplicação inclui `Dockerfile` e `docker-compose.yml` para execução conteinerizada.

```bash
# Subir a aplicação e o banco PostgreSQL
docker compose up --build -d
```

O `docker-compose.yml` sobe o banco PostgreSQL junto com a aplicação. A variável `JWT_SECRET` pode ser configurada no arquivo ou passada como variável de ambiente.

