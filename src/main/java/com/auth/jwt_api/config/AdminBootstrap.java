package com.auth.jwt_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.UserRepository;
import com.auth.jwt_api.services.AuthService;

/**
 * Cria um ADMIN inicial no startup a partir de variáveis de ambiente ({@code ADMIN_EMAIL}/
 * {@code ADMIN_PASSWORD}), permitindo provisionar organizadores depois. Sem essas variáveis, ou se
 * o usuário já existir, não faz nada — evitando credenciais fixas no repositório.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        authService.createUser(adminEmail, adminPassword, UserRole.ADMIN);
        log.info("ADMIN inicial criado a partir das variáveis de ambiente: {}", adminEmail);
    }
}
