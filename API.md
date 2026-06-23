# API Reference

Documentacao da API REST do projeto TicketFull.

## Base URL

- Local: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Autenticacao

A API usa JWT no header `Authorization`:

```http
Authorization: Bearer <access-token>
```

O refresh token e enviado em cookie `HttpOnly` chamado `refreshToken`, com path `/auth`.

Observacoes:

- O cookie de refresh e criado com `Secure=true`.
- Em ambiente local sem HTTPS, o fluxo de refresh pode exigir ajuste de ambiente ou teste via cliente que respeite cookies seguros.
- As rotas `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh` e `POST /auth/logout` sao publicas.

## Papeis

Valores aceitos para `role` no cadastro:

- `ADMIN`
- `USER`
- `ORGANIZER`
- `CUSTOMER`

Regras de acesso:

- `POST /events`: `ORGANIZER` ou `ADMIN`
- `POST /events/{eventId}/ticket-batches`: `ORGANIZER` ou `ADMIN`
- `POST /orders`: `CUSTOMER` ou `ADMIN`
- `POST /orders/{id}/pay`: `CUSTOMER` ou `ADMIN`
- `POST /tickets/{codeHash}/validate`: `ORGANIZER` ou `ADMIN`
- Demais rotas fora de `/auth` exigem autenticacao

## Endpoints

### 1. Registro

`POST /auth/register`

Cria um novo usuario.

Request:

```json
{
  "email": "organizador@ticketfull.com",
  "password": "senha123",
  "role": "ORGANIZER"
}
```

Responses:

- `201 Created`: usuario criado
- `400 Bad Request`: payload invalido
- `409 Conflict`: usuario ja existente

### 2. Login

`POST /auth/login`

Autentica o usuario e devolve access token. Tambem seta o cookie `refreshToken`.

Request:

```json
{
  "email": "organizador@ticketfull.com",
  "password": "senha123"
}
```

Response `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200000
}
```

Possiveis respostas:

- `200 OK`: autenticado
- `401 Unauthorized`: credenciais invalidas
- `429 Too Many Requests`: limite de tentativas de login excedido

### 3. Refresh de token

`POST /auth/refresh`

Renova o access token usando o cookie `refreshToken`. O refresh token tambem e rotacionado.

Response `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200000
}
```

Possiveis respostas:

- `200 OK`: token renovado
- `401 Unauthorized`: cookie ausente, invalido ou expirado

### 4. Logout

`POST /auth/logout`

Invalida o refresh token atual e limpa o cookie.

Responses:

- `204 No Content`: logout efetuado

### 5. Criar evento

`POST /events`

Requer `Authorization: Bearer <token>` com perfil `ORGANIZER` ou `ADMIN`.

Request:

```json
{
  "title": "Tech Conference 2026",
  "description": "Evento anual de tecnologia",
  "eventDate": "2026-09-10T19:30:00Z",
  "location": "Sao Paulo - SP"
}
```

Regras:

- `eventDate` deve estar no futuro
- o organizador vem do usuario autenticado, nao do corpo

Response `201 Created`:

```json
{
  "id": "8bd8d0b3-e0d7-47b0-bb0f-5a9900fa9772",
  "title": "Tech Conference 2026",
  "description": "Evento anual de tecnologia",
  "eventDate": "2026-09-10T19:30:00Z",
  "location": "Sao Paulo - SP",
  "organizerId": "7b5f9df6-00f0-4dfa-8d45-2a5fe59076f1",
  "createdAt": "2026-06-23T12:00:00Z",
  "updatedAt": "2026-06-23T12:00:00Z"
}
```

### 6. Listar eventos

`GET /events`

Requer autenticacao.

Response `200 OK`:

```json
[
  {
    "id": "8bd8d0b3-e0d7-47b0-bb0f-5a9900fa9772",
    "title": "Tech Conference 2026",
    "description": "Evento anual de tecnologia",
    "eventDate": "2026-09-10T19:30:00Z",
    "location": "Sao Paulo - SP",
    "organizerId": "7b5f9df6-00f0-4dfa-8d45-2a5fe59076f1",
    "createdAt": "2026-06-23T12:00:00Z",
    "updatedAt": "2026-06-23T12:00:00Z"
  }
]
```

### 7. Buscar evento por ID

`GET /events/{id}`

Requer autenticacao.

Response `200 OK`: mesmo formato de evento.

### 8. Criar lote de ingressos

`POST /events/{eventId}/ticket-batches`

Requer `Authorization: Bearer <token>` com perfil `ORGANIZER` ou `ADMIN`.

Request:

```json
{
  "name": "Lote Promocional",
  "price": 99.90,
  "totalCapacity": 300
}
```

Regras:

- `price` deve ser positivo
- `totalCapacity` deve ser positivo
- `availableSeats` e inicializado com o valor de `totalCapacity`

Response `201 Created`:

```json
{
  "id": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
  "eventId": "8bd8d0b3-e0d7-47b0-bb0f-5a9900fa9772",
  "name": "Lote Promocional",
  "price": 99.90,
  "totalCapacity": 300,
  "availableSeats": 300
}
```

### 9. Listar lotes de um evento

`GET /events/{eventId}/ticket-batches`

Requer autenticacao.

Response `200 OK`:

```json
[
  {
    "id": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
    "eventId": "8bd8d0b3-e0d7-47b0-bb0f-5a9900fa9772",
    "name": "Lote Promocional",
    "price": 99.90,
    "totalCapacity": 300,
    "availableSeats": 300
  }
]
```

### 10. Criar pedido

`POST /orders`

Requer `Authorization: Bearer <token>` com perfil `CUSTOMER` ou `ADMIN`.

Request:

```json
{
  "ticketBatchId": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
  "quantity": 2
}
```

Response `201 Created`:

```json
{
  "id": "92d4d090-922c-4d2c-a835-2604632bfdc5",
  "customerId": "3706d59e-3240-48e9-8eaa-3ff5ffd6f4d7",
  "eventId": "8bd8d0b3-e0d7-47b0-bb0f-5a9900fa9772",
  "status": "PENDING",
  "totalAmount": 199.80,
  "tickets": [
    {
      "id": "65f67d85-d805-4d3e-9ad0-098dcb1fa421",
      "ticketBatchId": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
      "codeHash": "0a5b2ef2c4d3...",
      "status": "VALID"
    },
    {
      "id": "cb5f07d7-e445-4d4a-8c88-1ea1a6dc77c2",
      "ticketBatchId": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
      "codeHash": "7aa2db5f8321...",
      "status": "VALID"
    }
  ],
  "createdAt": "2026-06-23T12:30:00Z"
}
```

Status possiveis de pedido:

- `PENDING`
- `PAID`
- `CANCELLED`

Status possiveis de ingresso:

- `VALID`
- `USED`

### 11. Listar meus pedidos

`GET /orders`

Requer autenticacao.

Response `200 OK`: lista de `OrderResponseDTO`.

### 12. Buscar meu pedido por ID

`GET /orders/{id}`

Requer autenticacao.

Response `200 OK`: mesmo formato de pedido.

### 13. Confirmar pagamento

`POST /orders/{id}/pay`

Requer `Authorization: Bearer <token>` com perfil `CUSTOMER` ou `ADMIN`.

Efeito:

- altera o pedido de `PENDING` para `PAID`

Response `200 OK`: mesmo formato de pedido, com `status` atualizado.

### 14. Validar ingresso

`POST /tickets/{codeHash}/validate`

Requer `Authorization: Bearer <token>` com perfil `ORGANIZER` ou `ADMIN`.

Efeito:

- valida o ingresso pelo `codeHash`
- marca o ticket como `USED`

Response `200 OK`:

```json
{
  "id": "65f67d85-d805-4d3e-9ad0-098dcb1fa421",
  "ticketBatchId": "5c97b01c-d70f-42ff-94ca-a0d303f83a14",
  "codeHash": "0a5b2ef2c4d3...",
  "status": "USED"
}
```

## Erros

A API usa `ProblemDetail` como formato padrao de erro.

Exemplo de erro de validacao (`400 Bad Request`):

```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/events",
  "errors": [
    {
      "field": "title",
      "message": "must not be blank"
    }
  ]
}
```

Exemplo de erro de autenticacao (`401 Unauthorized`):

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required to access this resource",
  "instance": "/orders"
}
```

Exemplo de rate limit (`429 Too Many Requests` em `/auth/login`):

```json
{
  "type": "about:blank",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many login attempts. Please try again later.",
  "instance": "/auth/login",
  "retryAfter": 300
}
```

## Exemplo de fluxo

1. Registrar um usuario `ORGANIZER`.
2. Fazer login e guardar o `accessToken`.
3. Criar um evento com `POST /events`.
4. Criar um lote com `POST /events/{eventId}/ticket-batches`.
5. Registrar um usuario `CUSTOMER`.
6. Fazer login como cliente.
7. Comprar ingressos com `POST /orders`.
8. Confirmar pagamento com `POST /orders/{id}/pay`.
9. Validar cada ingresso na entrada com `POST /tickets/{codeHash}/validate`.

## cURL rapido

Login:

```bash
curl -i -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"organizador@ticketfull.com","password":"senha123"}'
```

Criar evento:

```bash
curl -X POST http://localhost:8081/events \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <access-token>' \
  -d '{
    "title":"Tech Conference 2026",
    "description":"Evento anual de tecnologia",
    "eventDate":"2026-09-10T19:30:00Z",
    "location":"Sao Paulo - SP"
  }'
```