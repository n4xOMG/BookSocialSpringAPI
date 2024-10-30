package com.nix.request;

import com.nix.models.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
	private String email;
	private String password;
	private boolean rememberMe;
	private Role role;
}
