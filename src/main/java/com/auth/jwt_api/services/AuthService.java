package com.auth.jwt_api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.AuthenticationRequestDTO;
import com.auth.jwt_api.dtos.RegisterRequestDTO;
import com.auth.jwt_api.exceptions.InvalidCredentialsException;
import com.auth.jwt_api.exceptions.UserAlreadyExistsException;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.UserRepository;
import com.auth.jwt_api.security.TokenService;

@Service
public class AuthService {

    public record LoginResult(String accessToken, String refreshToken, long expiresIn) {
    }

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${api.security.token.expiration}")
    private long accessTokenExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService,
                       TokenBlacklistService tokenBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public LoginResult login(AuthenticationRequestDTO request) {
        try {
            var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
            var authentication = authenticationManager.authenticate(authToken);
            var user = (User) authentication.getPrincipal();

            String accessToken = tokenService.generateToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

            return new LoginResult(accessToken, refreshToken, accessTokenExpirationMs / 1000);
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }
    }

    /** Auto-registro público: cria sempre um CUSTOMER (papéis privilegiados são provisionados por um ADMIN). */
    @Transactional
    public void register(RegisterRequestDTO request) {
        createUser(request.email(), request.password(), UserRole.CUSTOMER);
    }

    /** Criação de usuário com papel explícito — uso restrito a operações administrativas. */
    @Transactional
    public void createUser(String email, String rawPassword, UserRole role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        User newUser = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();

        userRepository.save(newUser);
    }

    public LoginResult refreshToken(String refreshToken) {
        return refreshTokenService.rotateRefreshToken(refreshToken, accessTokenExpirationMs / 1000);
    }

    /** Encerra a sessão: revoga o access token (lista de bloqueio) e remove o refresh token. */
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            String jti = tokenService.extractTokenId(accessToken);
            tokenBlacklistService.revoke(jti, tokenService.extractExpiration(accessToken));
        }
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }
    }
}
