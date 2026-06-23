package com.auth.jwt_api.payments;

import com.auth.jwt_api.models.Order;

/**
 * Porta (Ports &amp; Adapters) para o provedor de pagamento. O domínio depende desta abstração;
 * os adaptadores concretos ({@code FakePaymentGateway} para dev/test, um {@code StripePaymentGateway}
 * em produção) implementam a integração real sem que o {@code OrderService} precise conhecê-los.
 */
public interface PaymentGateway {

    /** Cria uma intenção de pagamento para o pedido e retorna os dados de checkout. */
    PaymentIntent createPaymentIntent(Order order);

    /** Solicita o estorno de um pagamento já confirmado. */
    void refund(String paymentIntentId);

    /**
     * Verifica a assinatura e traduz o payload bruto do webhook para um evento normalizado.
     *
     * @param payload         corpo bruto da requisição (necessário para validar a assinatura)
     * @param signatureHeader cabeçalho de assinatura enviado pelo provedor (pode ser nulo no Fake)
     */
    PaymentWebhookEvent parseWebhookEvent(String payload, String signatureHeader);
}
