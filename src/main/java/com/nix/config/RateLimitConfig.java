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
		Bandwidth limit = Bandwidth.classic(150, Refill.greedy(150, Duration.ofMinutes(1)));
		return Bucket.builder().addLimit(limit).build();
	}
}