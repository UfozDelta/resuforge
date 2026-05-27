package com.resumepipeline.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter for /api/register.
 * Allows 5 registration attempts per hour per IP.
 */
@Component
public class RegisterRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String ip) {
        return buckets.computeIfAbsent(ip, k -> newBucket()).tryConsume(1);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofHours(1))
                        .build())
                .build();
    }
}
