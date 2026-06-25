package com.auth.jwt_api.notifications;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.auth.jwt_api.events.OrderPaidEvent;

/**
 * Consome {@link OrderPaidEvent} de forma assíncrona e somente após o commit da transação de pagamento,
 * garantindo que a notificação só é enviada quando o pedido foi de fato persistido como PAID.
 */
@Component
public class OrderNotificationListener {

    private final NotificationSender notificationSender;

    public OrderNotificationListener(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @Async
    @TransactionalEventListener
    public void onOrderPaid(OrderPaidEvent event) {
        notificationSender.sendOrderConfirmation(event.customerEmail(), event.orderId(), event.ticketCount());
    }
}
