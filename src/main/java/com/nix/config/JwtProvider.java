package com.nix.config;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT token provider for generating and parsing JWT tokens.
 * Uses instance methods for proper dependency injection.
 */
@Component
public class JwtProvider {

	private static final long JWT_EXPIRATION = 86400000; // Default 1 day
	private static final long REMEMBER_ME_EXPIRATION = 604800000; // 7 days

	private final SecretKey key;

	public JwtProvider(@Value("${jwt.secret.key}") String secretKey) {
		// Initialize the key directly from injected value
		if (secretKey == null || secretKey.isBlank()) {
			throw new IllegalStateException(
					"JWT secret key is not configured. Set jwt.secret.key in your environment.");
		}
		this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	/**
	 * Generate a JWT token for the authenticated user.
	 */
	public String generateToken(Authentication auth, boolean rememberMe) {
		long expirationTime = rememberMe ? REMEMBER_ME_EXPIRATION : JWT_EXPIRATION;
		return Jwts.builder()
				.issuer("nix")
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + expirationTime))
				.claim("email", auth.getName())
				.claim("role", auth.getAuthorities())
				.signWith(key)
				.compact();
	}

	/**
	 * Extract email from JWT token.
	 */
	public String getEmailFromJwtToken(String jwt) {
		if (jwt == null || !jwt.startsWith("Bearer ") || jwt.length() <= 7) {
			throw new JwtException("Invalid token format");
		}
		jwt = jwt.substring(7);

		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(jwt)
				.getPayload();

		return String.valueOf(claims.get("email"));
	}

	/**
	 * Extract role from JWT token.
	 * Note: With database role validation, this method is now primarily used
	 * for debugging/logging purposes since actual roles come from the database.
	 */
	public String getRoleFromJwtToken(String jwt) {
		if (jwt == null || !jwt.startsWith("Bearer ") || jwt.length() <= 7) {
			throw new JwtException("Invalid token format");
		}
		jwt = jwt.substring(7);

		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(jwt)
				.getPayload();

		Object rolesClaim = claims.get("role");
		if (rolesClaim == null) {
			throw new JwtException("No role claim found in token");
		}

		@SuppressWarnings("unchecked")
		java.util.List<java.util.Map<String, String>> authorities = (java.util.List<java.util.Map<String, String>>) rolesClaim;

		if (authorities == null || authorities.isEmpty()) {
			throw new JwtException("No authorities found in token");
		}

		String roleName = authorities.get(0).get("authority");

		if (roleName == null || roleName.isEmpty()) {
			throw new JwtException("Invalid role format in token");
		}

		return roleName;
	}
}
