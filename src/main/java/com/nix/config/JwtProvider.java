package com.nix.config;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtProvider {
	private static SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());
	private static final long JWT_EXPIRATION = 86400000; // Default 1 day
    private static final long REMEMBER_ME_EXPIRATION = 604800000; // 7 days
	public static String generateToken(Authentication auth, boolean rememberMe) {
		long expirationTime = rememberMe ? REMEMBER_ME_EXPIRATION : JWT_EXPIRATION;
	    String jwt = Jwts.builder()
	            .setIssuer("nix")
	            .setIssuedAt(new Date())
	            .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
	            .claim("email", auth.getName())
	            .claim("role",  auth.getAuthorities())
	            .signWith(key)
	            .compact();
	    return jwt;
	}

	public static String getEmailFromJwtToken(String jwt) {
		jwt = jwt.substring(7);
		
		Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
		
		String email=String.valueOf(claims.get("email"));
		
		return email;
		
	}
	public static String getRoleFromJwtToken(String jwt) {
	    jwt = jwt.substring(7);
	    Claims claims = Jwts.parser()
	        .setSigningKey(key).build()
	        .parseClaimsJws(jwt)
	        .getBody();
	    
	    // Get the list of authorities from the "role" claim
	    List<Map<String, String>> authorities = (List<Map<String, String>>) claims.get("role");

	    // Extract the role name from the first authority in the list
	    String roleName = authorities.get(0).get("authority");

	    return roleName;
	}

}
