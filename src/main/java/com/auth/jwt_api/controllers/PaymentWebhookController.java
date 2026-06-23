package com.auth.jwt_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.payments.PaymentGateway;
import com.auth.jwt_api.payments.PaymentWebhookEvent;
import com.auth.jwt_api.services.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Recebe os webhooks do gateway de pagamento. Endpoint público (chamado pelo provedor, não pelo
 * front-end autenticado); o adaptador do gateway é responsável por verificar a assinatura. O
 * processamento é idempotente em {@link OrderService#confirmPayment(String)}.
 */
@RestController
@RequestMapping("/webhooks/payments")
@Tag(name = "Webhooks", description = "Callbacks do gateway de pagamento")
public class PaymentWebhookController {

    private final PaymentGateway paymentGateway;
    private final OrderService orderService;

    public PaymentWebhookController(PaymentGateway paymentGateway, OrderService orderService) {
        this.paymentGateway = paymentGateway;
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Receber evento de pagamento do gateway (confirma o pedido — PENDING -> PAID)")
    public ResponseEntity<Void> handle(@RequestBody String payload,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        PaymentWebhookEvent event = paymentGateway.parseWebhookEvent(payload, signature);
        if (event.isPaymentSucceeded() && event.paymentIntentId() != null) {
            orderService.confirmPayment(event.paymentIntentId());
        }
        // Sempre 200 para eventos reconhecidos, evitando reentregas desnecessárias do provedor.
        return ResponseEntity.ok().build();
    }
}
