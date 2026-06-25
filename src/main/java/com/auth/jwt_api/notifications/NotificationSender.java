package com.auth.jwt_api.notifications;

/**
 * Porta de envio de notificações ao cliente. Em produção, um adaptador real enviaria e-mail (com o QR
 * Code do ingresso); em dev/test, a implementação padrão apenas registra em log.
 */
public interface NotificationSender {

    void sendOrderConfirmation(String to, java.util.UUID orderId, int ticketCount);
}
