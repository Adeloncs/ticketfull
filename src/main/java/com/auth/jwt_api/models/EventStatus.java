package com.auth.jwt_api.models;

public enum EventStatus {
    /** Rascunho: visível apenas ao organizador; não aparece na busca pública nem aceita vendas. */
    DRAFT,
    /** Publicado: visível na busca pública e disponível para compra. */
    PUBLISHED,
    /** Cancelado: encerrado; não aceita novas vendas. */
    CANCELLED
}
