package com.auth.jwt_api.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

@Service
public class RateLimiterService {

    @Value("${rate-limit.capacity:5}")
    private int capacity;

    @Value("${rate-limit.refill-minutes:5}")
    private int refillMinutes;

    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

    public ConsumptionProbe tryConsume(String key) {
        Bucket bucket = cache.computeIfAbsent(key, _ -> newBucket());
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(refillMinutes))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
