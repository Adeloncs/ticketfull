package com.auth.jwt_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.services.OrderService;

@SpringBootTest
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private OrderService orderService;

    private MockMvc mockMvc;

    private final String validBody = "{ \"ticketBatchId\": \"" + UUID.randomUUID() + "\", \"quantity\": 2 }";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private RequestPostProcessor as(UserRole role) {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").role(role).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("POST /orders: CUSTOMER compra -> 201")
    void create_shouldReturn201_forCustomer() throws Exception {
        OrderResponseDTO dto = new OrderResponseDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OrderStatus.PENDING, new BigDecimal("200.00"), List.of(), null);
        when(orderService.create(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/orders").with(as(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /orders: ORGANIZER -> 403 (role)")
    void create_shouldReturn403_forOrganizer() throws Exception {
        mockMvc.perform(post("/orders").with(as(UserRole.ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /orders: sem autenticação -> 401")
    void create_shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(validBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /orders: quantity inválido -> 400")
    void create_shouldReturn400_whenQuantityInvalid() throws Exception {
        String invalid = "{ \"ticketBatchId\": \"" + UUID.randomUUID() + "\", \"quantity\": 0 }";
        mockMvc.perform(post("/orders").with(as(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }
}
