package com.auth.jwt_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita tarefas agendadas (ex.: expiração de reservas). Desativado no profile de teste para que
 * o scheduler não dispare durante os testes de integração — a lógica é exercida diretamente.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
