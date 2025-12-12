package com.nix.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

@Configuration
public class RateLimitConfig {

	// In-memory storage for buckets
	private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	@Bean
	public Map<String, Bucket> buckets() {
		return buckets;
	}

	public Bucket createBucket() {
		Bandwidth limit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1)));
		return Bucket.builder().addLimit(limit).build();
	}

	/**
	 * Creates a bucket with stricter limits for authentication endpoints
	 * to prevent brute force attacks and email bombing.
	 * 
	 * Limits:
	 * - 5 requests per minute (prevents rapid brute force)
	 * - 20 requests per hour (prevents sustained attacks)
	 */
	public Bucket createAuthBucket() {
		// Short-term limit: 5 requests per minute
		Bandwidth shortTermLimit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1)));

		// Long-term limit: 20 requests per hour
		Bandwidth longTermLimit = Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofHours(1)));

		return Bucket.builder()
				.addLimit(shortTermLimit)
				.addLimit(longTermLimit)
				.build();
	}
}