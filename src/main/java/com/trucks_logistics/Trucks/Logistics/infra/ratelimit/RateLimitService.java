package com.trucks_logistics.Trucks.Logistics.infra.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;

@Service
public class RateLimitService {
    private final ConcurrentHashMap<String, Bucket> resendBuckets = new ConcurrentHashMap<>();

    public boolean allowResendVerification(String email) {
        Bucket bucket = resendBuckets.computeIfAbsent(email, this::createBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(String email) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(3).refillGreedy(3, Duration.ofHours(1)).initialTokens(3))
                .build();
    }
}
