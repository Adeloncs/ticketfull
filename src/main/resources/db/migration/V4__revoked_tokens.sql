-- Lista de bloqueio de tokens de acesso (revogação no logout).
-- jti = identificador único do JWT; expires_at permite limpar entradas já expiradas.
CREATE TABLE revoked_tokens (
    jti        VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
