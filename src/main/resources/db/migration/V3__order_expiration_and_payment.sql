-- Reserva com TTL + correlação de pagamento:
-- expires_at  -> prazo da reserva; pedidos PENDING vencidos são expirados e devolvem assentos
-- payment_intent_id -> identificador do PaymentIntent no gateway (chave do webhook idempotente)
ALTER TABLE orders ADD COLUMN expires_at        TIMESTAMP WITH TIME ZONE;
ALTER TABLE orders ADD COLUMN payment_intent_id VARCHAR(255);

ALTER TABLE orders ADD CONSTRAINT uq_orders_payment_intent_id UNIQUE (payment_intent_id);

-- Acelera a varredura periódica de reservas vencidas (status PENDING + expires_at no passado)
CREATE INDEX idx_orders_status_expires_at ON orders (status, expires_at);
