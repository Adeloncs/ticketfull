package com.auth.jwt_api.payments;

/**
 * Evento normalizado recebido do gateway de pagamento. Cada adaptador (Stripe, Fake, ...)
 * é responsável por verificar a assinatura e traduzir o payload do provedor para este formato.
 *
 * @param type            tipo do evento (ex.: {@link #PAYMENT_SUCCEEDED})
 * @param paymentIntentId PaymentIntent ao qual o evento se refere
 */
public record PaymentWebhookEvent(String type, String paymentIntentId) {

    public static final String PAYMENT_SUCCEEDED = "payment_intent.succeeded";

    public boolean isPaymentSucceeded() {
        return PAYMENT_SUCCEEDED.equals(type);
    }
}
