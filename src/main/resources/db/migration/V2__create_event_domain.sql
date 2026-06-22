-- Domínio EventFlow: eventos, lotes de ingressos, pedidos e ingressos
CREATE TABLE events (
    id           UUID PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    event_date   TIMESTAMP WITH TIME ZONE NOT NULL,
    location     VARCHAR(255) NOT NULL,
    organizer_id UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ticket_batches (
    id              UUID PRIMARY KEY,
    event_id        UUID NOT NULL REFERENCES events(id),
    name            VARCHAR(255) NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    total_capacity  INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE orders (
    id           UUID PRIMARY KEY,
    customer_id  UUID NOT NULL REFERENCES users(id),
    event_id     UUID NOT NULL REFERENCES events(id),
    status       VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tickets (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL REFERENCES orders(id),
    ticket_batch_id UUID NOT NULL REFERENCES ticket_batches(id),
    code_hash       VARCHAR(255) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
