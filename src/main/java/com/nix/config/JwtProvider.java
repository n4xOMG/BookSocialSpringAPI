package com.nix.config;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {
	private static SecretKey getKey() {
		return Keys.hmacShaKeyFor(JwtConstant.getSecretKey().getBytes());
	}

	private static final long JWT_EXPIRATION = 86400000; // Default 1 day
	private static final long REMEMBER_ME_EXPIRATION = 604800000; // 7 days

	public static String generateToken(Authentication auth, boolean rememberMe) {
		long expirationTime = rememberMe ? REMEMBER_ME_EXPIRATION : JWT_EXPIRATION;
		String jwt = Jwts.builder()
				.setIssuer("nix")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expirationTime))
				.claim("email", auth.getName())
				.claim("role", auth.getAuthorities())
				.signWith(getKey())
				.compact();
		return jwt;
	}

	public static String getEmailFromJwtToken(String jwt) {
		if (jwt == null || !jwt.startsWith("Bearer ") || jwt.length() <= 7) {
			throw new JwtException("Invalid token format");
		}
		jwt = jwt.substring(7);

		Claims claims = Jwts.parser().setSigningKey(getKey()).build().parseClaimsJws(jwt).getBody();

		String email = String.valueOf(claims.get("email"));

		return email;

	}

	public static String getRoleFromJwtToken(String jwt) {
		if (jwt == null || !jwt.startsWith("Bearer ") || jwt.length() <= 7) {
			throw new JwtException("Invalid token format");
		}
		jwt = jwt.substring(7);
		Claims claims = Jwts.parser()
				.setSigningKey(getKey()).build()
				.parseClaimsJws(jwt)
				.getBody();

		// Get the list of authorities from the "role" claim
		Object rolesClaim = claims.get("role");
		if (rolesClaim == null) {
			throw new JwtException("No role claim found in token");
		}

		List<Map<String, String>> authorities = (List<Map<String, String>>) rolesClaim;

		if (authorities == null || authorities.isEmpty()) {
			throw new JwtException("No authorities found in token");
		}

		// Extract the role name from the first authority in the list
		String roleName = authorities.get(0).get("authority");

		if (roleName == null || roleName.isEmpty()) {
			throw new JwtException("Invalid role format in token");
		}

		return roleName;
	}

}
