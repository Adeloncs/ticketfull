package com.auth.jwt_api.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.auth.jwt_api.exceptions.TooManyRequestsException;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/auth/login";

    private final RateLimiterService rateLimiterService;
    private final HandlerExceptionResolver resolver;

    public RateLimitingFilter(RateLimiterService rateLimiterService,
                               @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.rateLimiterService = rateLimiterService;
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        ConsumptionProbe probe = rateLimiterService.tryConsume(clientIp);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            TooManyRequestsException exception = new TooManyRequestsException(probe.getNanosToWaitForRefill());
            resolver.resolveException(request, response, null, exception);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
