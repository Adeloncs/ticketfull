package com.auth.jwt_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita o processamento assíncrono (ex.: envio de notificações fora da thread da requisição). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
