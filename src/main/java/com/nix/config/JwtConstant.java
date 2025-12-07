package com.nix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class JwtConstant {
	public static final String JWT_HEADER = "Authorization";

	@Value("${jwt.secret.key}")
	private String jwtSecretKey;

	private static String SECRET_KEY;

	@PostConstruct
	public void init() {
		SECRET_KEY = jwtSecretKey;
	}

	public static String getSecretKey() {
		return SECRET_KEY;
	}
}
