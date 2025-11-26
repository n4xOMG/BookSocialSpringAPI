package com.nix.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Rate limiter specifically for authentication endpoints to prevent brute force
 * attacks
 * and email bombing. This interceptor applies stricter limits than the general
 * rate limiter.
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthRateLimitInterceptor.class);

    @Autowired
    private Map<String, Bucket> buckets;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String clientKey = getClientKey(request);
        String endpoint = request.getRequestURI();

        // Use auth-specific bucket with stricter limits
        String authBucketKey = "auth:" + clientKey;
        Bucket bucket = buckets.computeIfAbsent(authBucketKey, k -> rateLimitConfig.createAuthBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        // Rate limit exceeded - log suspicious activity
        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        logger.warn("Rate limit exceeded for authentication endpoint. IP: {}, Endpoint: {}, Wait: {}s",
                clientKey, endpoint, waitSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.addHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(waitSeconds));
        response.getWriter().write(String.format(
                "{\"message\": \"Too many authentication attempts. Please try again in %d seconds.\", \"success\": false}",
                waitSeconds));
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
                logger.debug("Using IP from header {}: {}", header, ip);
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        logger.debug("Using remote address: {}", remoteAddr);
        return remoteAddr;
    }
}
