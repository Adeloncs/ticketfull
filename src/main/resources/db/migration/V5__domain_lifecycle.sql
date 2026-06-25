-- Features de domínio: ciclo de vida do evento, janela de vendas do lote e transferência de ingresso

-- Ciclo de vida do evento (DRAFT/PUBLISHED/CANCELLED). Eventos pré-existentes ficam PUBLISHED.
ALTER TABLE events ADD COLUMN status VARCHAR(20);
UPDATE events SET status = 'PUBLISHED';
ALTER TABLE events ALTER COLUMN status SET NOT NULL;

-- Janela de vendas do lote (nulo = sem restrição)
ALTER TABLE ticket_batches ADD COLUMN sales_start_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ticket_batches ADD COLUMN sales_end_at   TIMESTAMP WITH TIME ZONE;

-- Detentor do ingresso após transferência (nulo = dono é o cliente do pedido)
ALTER TABLE tickets ADD COLUMN holder_id UUID REFERENCES users(id);
