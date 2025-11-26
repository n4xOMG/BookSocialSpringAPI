package com.nix.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 * Applies different rate limiting strategies to different endpoint groups.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private AuthRateLimitInterceptor authRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply strict rate limiting to authentication endpoints
        // These are vulnerable to brute force and email bombing attacks
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns(
                        "/auth/signin", // Login endpoint - brute force target
                        "/auth/signup", // Registration endpoint
                        "/auth/forgot-password", // Email bombing target
                        "/auth/reset-password", // Password reset
                        "/auth/verify-otp" // OTP verification
                )
                .order(1); // Execute first

        // Apply general rate limiting to all other endpoints
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/signin",
                        "/auth/signup",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/auth/verify-otp")
                .order(2); // Execute after auth rate limiter
    }
}
