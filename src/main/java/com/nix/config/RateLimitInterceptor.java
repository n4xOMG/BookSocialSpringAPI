package com.nix.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

	@Autowired
	private Map<String, Bucket> buckets;

	@Autowired
	private RateLimitConfig rateLimitConfig;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// Identify client (e.g., by IP or authenticated user)
		String clientKey = getClientKey(request);

		// Get or create bucket for this client
		Bucket bucket = buckets.computeIfAbsent(clientKey, k -> rateLimitConfig.createBucket());

		// Try to consume a token
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			// Add headers to inform client of remaining quota
			response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
			return true;
		}

		// Rate limit exceeded
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
		response.addHeader("X-Rate-Limit-Retry-After-Seconds",
				String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
		response.getWriter().write("Rate limit exceeded. Try again later.");
		return false;
	}

	private String getClientKey(HttpServletRequest request) {
		// Option 1: Rate limit by IP address
		String clientIp = request.getRemoteAddr();

		return clientIp;
	}
}
