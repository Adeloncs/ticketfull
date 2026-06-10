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

    @Value("${api.security.token.expiration}")
    private long accessTokenExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       TokenService tokenService,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
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

    @Transactional
    public void register(RegisterRequestDTO request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        String encryptedPassword = passwordEncoder.encode(request.password());

        User newUser = User.builder()
                .email(request.email())
                .password(encryptedPassword)
                .role(request.role())
                .build();

        userRepository.save(newUser);
    }

    public LoginResult refreshToken(String refreshToken) {
        return refreshTokenService.rotateRefreshToken(refreshToken, accessTokenExpirationMs / 1000);
    }
}
