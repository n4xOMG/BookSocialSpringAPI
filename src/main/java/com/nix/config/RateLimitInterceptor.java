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
		String clientKey = getClientKey(request);

		Bucket bucket = buckets.computeIfAbsent(clientKey, k -> rateLimitConfig.createBucket());

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
			return true;
		}

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.addHeader("X-Rate-Limit-Retry-After-Seconds",
				String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
		response.getWriter().write("Rate limit exceeded. Try again later.");
		return false;
	}

	/**
	 * Get client identifier. Checks proxy headers to prevent bypass attempts.
	 * Falls back to remote address if proxy headers are not present.
	 */
	private String getClientKey(HttpServletRequest request) {
		// Check common proxy headers (in order of preference)
		String[] headerNames = {
				"X-Forwarded-For",
				"X-Real-IP",
				"Proxy-Client-IP",
				"WL-Proxy-Client-IP",
				"HTTP_X_FORWARDED_FOR",
				"HTTP_X_FORWARDED",
				"HTTP_X_CLUSTER_CLIENT_IP",
				"HTTP_CLIENT_IP",
				"HTTP_FORWARDED_FOR",
				"HTTP_FORWARDED"
		};

		for (String header : headerNames) {
			String ip = request.getHeader(header);
			if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
				// X-Forwarded-For can contain multiple IPs, take the first one (client IP)
				int commaIndex = ip.indexOf(',');
				if (commaIndex != -1) {
					ip = ip.substring(0, commaIndex).trim();
				}
				return ip;
			}
		}

		// Fallback to remote address
		return request.getRemoteAddr();
	}
}
