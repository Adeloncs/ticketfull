package com.auth.jwt_api.notifications;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implementação padrão (dev/test): registra a notificação em log no lugar de enviar e-mail real. */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void sendOrderConfirmation(String to, UUID orderId, int ticketCount) {
        log.info("[Notificação] Confirmação de pagamento enviada para {}: pedido {} com {} ingresso(s) + QR Code",
                to, orderId, ticketCount);
    }
}
