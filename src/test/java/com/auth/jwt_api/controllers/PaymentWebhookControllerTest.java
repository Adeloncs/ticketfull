package com.auth.jwt_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.payments.PaymentGateway;
import com.auth.jwt_api.payments.PaymentWebhookEvent;
import com.auth.jwt_api.services.OrderService;

@SpringBootTest
@ActiveProfiles("test")
class PaymentWebhookControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("POST /webhooks/payments: público (sem auth) e confirma o pedido do PaymentIntent")
    void handle_shouldConfirmPayment_whenSucceeded() throws Exception {
        String payload = "{\"type\":\"payment_intent.succeeded\",\"paymentIntentId\":\"pi_123\"}";
        when(paymentGateway.parseWebhookEvent(any(), any()))
                .thenReturn(new PaymentWebhookEvent(PaymentWebhookEvent.PAYMENT_SUCCEEDED, "pi_123"));

        mockMvc.perform(post("/webhooks/payments")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        verify(orderService).confirmPayment(eq("pi_123"));
    }

    @Test
    @DisplayName("POST /webhooks/payments: ignora eventos não relacionados a sucesso de pagamento")
    void handle_shouldIgnore_whenNotSucceeded() throws Exception {
        when(paymentGateway.parseWebhookEvent(any(), any()))
                .thenReturn(new PaymentWebhookEvent("payment_intent.created", "pi_123"));

        mockMvc.perform(post("/webhooks/payments")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(orderService, never()).confirmPayment(any());
    }
}
