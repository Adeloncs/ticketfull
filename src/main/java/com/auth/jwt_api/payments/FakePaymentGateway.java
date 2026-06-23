package com.auth.jwt_api.payments;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.auth.jwt_api.models.Order;

import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de pagamento para desenvolvimento e testes. Simula um provedor (estilo Stripe) gerando
 * PaymentIntents e aceitando webhooks sem dependência externa nem credenciais. Um adaptador real
 * ({@code StripePaymentGateway}) implementaria a mesma porta {@link PaymentGateway} e seria ativado
 * via {@code app.payments.gateway=stripe}.
 *
 * <p>Para confirmar um pagamento localmente, basta enviar para {@code POST /webhooks/payments} um
 * corpo no formato:
 * <pre>{"type":"payment_intent.succeeded","paymentIntentId":"pi_..."}</pre>
 */
@Component
@ConditionalOnProperty(name = "app.payments.gateway", havingValue = "fake", matchIfMissing = true)
public class FakePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);
    private static final String CURRENCY = "BRL";

    private final ObjectMapper objectMapper;

    public FakePaymentGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentIntent createPaymentIntent(Order order) {
        String id = "pi_" + UUID.randomUUID().toString().replace("-", "");
        String clientSecret = id + "_secret_" + UUID.randomUUID().toString().replace("-", "");
        log.info("[FakeGateway] PaymentIntent {} criado para pedido {} no valor de {} {}",
                id, order.getId(), order.getTotalAmount(), CURRENCY);
        return new PaymentIntent(id, clientSecret, order.getTotalAmount(), CURRENCY);
    }

    @Override
    public void refund(String paymentIntentId) {
        log.info("[FakeGateway] Estorno solicitado para PaymentIntent {}", paymentIntentId);
    }

    @Override
    public PaymentWebhookEvent parseWebhookEvent(String payload, String signatureHeader) {
        // Um adaptador real verificaria a assinatura (signatureHeader) contra o segredo do webhook.
        try {
            FakeWebhookPayload parsed = objectMapper.readValue(payload, FakeWebhookPayload.class);
            return new PaymentWebhookEvent(parsed.type(), parsed.paymentIntentId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload de webhook inválido", e);
        }
    }

    /** Formato simplificado do webhook do gateway fake: {@code {"type":..., "paymentIntentId":...}}. */
    private record FakeWebhookPayload(String type, String paymentIntentId) {
    }
}
