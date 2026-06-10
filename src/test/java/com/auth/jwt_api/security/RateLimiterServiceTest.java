package com.auth.jwt_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.bucket4j.ConsumptionProbe;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "capacity", 5);
        ReflectionTestUtils.setField(rateLimiterService, "refillMinutes", 5);
    }

    @Test
    @DisplayName("tryConsume: primeira tentativa deve retornar probe consumido")
    void tryConsume_shouldReturnConsumed_onFirstAttempt() {
        ConsumptionProbe probe = rateLimiterService.tryConsume("192.168.0.1");

        assertThat(probe.isConsumed()).isTrue();
        assertThat(probe.getRemainingTokens()).isEqualTo(4);
    }

    @Test
    @DisplayName("tryConsume: deve bloquear após esgotar o limite exato de tokens")
    void tryConsume_shouldBlock_afterExhaustingCapacity() {
        String ip = "192.168.0.2";

        for (int i = 0; i < 5; i++) {
            ConsumptionProbe probe = rateLimiterService.tryConsume(ip);
            assertThat(probe.isConsumed())
                    .as("Tentativa %d deve ser autorizada", i + 1)
                    .isTrue();
        }

        ConsumptionProbe blockedProbe = rateLimiterService.tryConsume(ip);

        assertThat(blockedProbe.isConsumed()).isFalse();
        assertThat(blockedProbe.getNanosToWaitForRefill()).isPositive();
    }

    @Test
    @DisplayName("tryConsume: esgotar os tokens de IP_A não deve afetar o bucket de IP_B")
    void tryConsume_shouldIsolateBuckets_perIp() {
        String ipA = "10.0.1.1";
        String ipB = "10.0.1.2";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(ipA);
        }

        ConsumptionProbe probeA = rateLimiterService.tryConsume(ipA);
        assertThat(probeA.isConsumed()).isFalse();

        ConsumptionProbe probeB = rateLimiterService.tryConsume(ipB);
        assertThat(probeB.isConsumed()).isTrue();
    }
}
