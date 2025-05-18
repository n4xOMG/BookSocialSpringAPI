package com.nix.controller;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.config.JwtProvider;
import com.nix.exception.AccountException;
import com.nix.models.User;
import com.nix.request.LoginRequest;
import com.nix.response.AuthResponse;
import com.nix.service.CustomUserDetailsService;
import com.nix.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AuthController {
	@Autowired
	private PasswordEncoder passEncoder;

	@Autowired
	private UserService userService;

	@Autowired
	private CustomUserDetailsService customUserDetails;

	@Value("${frontend.url}")
	private String frontendUrl;

	@PostMapping("/auth/signup")
	public ResponseEntity<AuthResponse> signUp(@RequestBody User user, HttpServletRequest request) throws Exception {
		User isExistedUser = userService.findUserByEmail(user.getEmail());
		if (isExistedUser != null) {
			AuthResponse authRes = new AuthResponse(null, "Email is already exist!");
			return new ResponseEntity<>(authRes, HttpStatus.NOT_ACCEPTABLE);
		}

		User newUser = userService.register(user);
		Authentication auth = new UsernamePasswordAuthenticationToken(newUser.getEmail(), newUser.getPassword());

		String token = JwtProvider.generateToken(auth, false);
		AuthResponse authRes = new AuthResponse(token, "Sign up succeed!");

		return new ResponseEntity<>(authRes, HttpStatus.OK);

	}

	@PostMapping("/auth/signin")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginReq) {
		boolean rememberMe = loginReq.isRememberMe();
		Authentication auth = null;
		try {
			auth = authenticate(loginReq.getEmail(), loginReq.getPassword());
		} catch (BadCredentialsException e) {
			System.out.println("Authentication failed: " + e.getMessage());
			AuthResponse authRes = new AuthResponse(null, "Invalid email/password!");
			return new ResponseEntity<>(authRes, HttpStatus.FORBIDDEN);
		} catch (AccountException e) {
			// Handle unverified account exception
			System.out.println("Account Exception: " + e.getMessage());
			AuthResponse authRes = new AuthResponse(null, e.getMessage());
			return new ResponseEntity<>(authRes, HttpStatus.FORBIDDEN);
		}

		if (auth == null) {
			AuthResponse authRes = new AuthResponse(null, "Invalid email/password!");
			return new ResponseEntity<>(authRes, HttpStatus.UNAUTHORIZED);
		}

		String token = JwtProvider.generateToken(auth, rememberMe);
		userService.updateUserLastLoginDate(loginReq.getEmail());
		AuthResponse authRes = new AuthResponse(token, "Login succeeded!");
		return ResponseEntity.ok(authRes);
	}

	@PostMapping("/auth/forgot-password")
	public ResponseEntity<?> sendForgotPasswordMail(@RequestBody Map<String, String> request,
			HttpServletRequest httpRequest) throws UnsupportedEncodingException, MessagingException {
		String email = request.get("email");
		User user = userService.findUserByEmail(email);
		if (user == null) {
			return ResponseEntity.badRequest().body("Email not found!");
		}

		userService.sendForgotPasswordMail(user);

		return ResponseEntity.ok("Reset password link has been sent to your email.");
	}

	@PostMapping("/api/user/update-email")
	public ResponseEntity<?> updateUserEmail(@RequestHeader("Authorization") String jwt,
			@RequestBody Map<String, String> request) {
		User user = userService.findUserByJwt(jwt);

		user.setEmail(request.get("email")); // Now update email
		user.setIsVerified(true); // Mark email as verified
		user.setVerificationCode(null); // Clear OTP
		userService.updateUser(user.getId(), user);

		return ResponseEntity.ok("Email verified successfully");
	}

	@PostMapping("/auth/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
		try {
			User user = userService.findUserByEmail(request.get("email"));
			String password = request.get("password");
			if (user == null) {
				return ResponseEntity.badRequest().body("User not found!");
			}

			userService.updateUserPassword(password, user);

			return ResponseEntity.ok("Password has been reset successfully.");
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/auth/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		String otp = request.get("otp");
		String context = request.get("context");
		User user = userService.findUserByEmail(email);
		if (user == null) {
			return ResponseEntity.badRequest().body("User not found!");
		}

		if (!otp.equals(user.getVerificationCode())) {
			return ResponseEntity.badRequest().body("Invalid or expired OTP!");
		}

		user.setIsVerified(true);
		user.setVerificationCode(null);
		userService.updateUser(user.getId(), user);

		if ("register".equals(context)) {
			return ResponseEntity.ok("OTP verified successfully. Registration complete.");
		} else if ("resetPassword".equals(context)) {
			return ResponseEntity.ok("OTP verified successfully. Please reset your password.");
		} else if ("updateProfile".equals(context)) {
			return ResponseEntity.ok("OTP verified successfully. Update profile complete");
		} else {
			return ResponseEntity.badRequest().body("Invalid context!");
		}
	}

	public Authentication authenticate(String email, String password) {
		// Load user details once
		UserDetails userDetails = customUserDetails.loadUserByUsername(email);
		if (userDetails == null) {
			handleFailedLoginAttempt(email);
			throw new BadCredentialsException("Invalid email or password");
		}

		// Validate password
		if (!passEncoder.matches(password, userDetails.getPassword())) {
			handleFailedLoginAttempt(email);
			throw new BadCredentialsException("Invalid email or password");
		}

		// Fetch User entity only if needed for additional checks
		User user = userService.findUserByEmail(email);
		if (user == null) {
			handleFailedLoginAttempt(email);
			throw new BadCredentialsException("Invalid email or password");
		}

		// Check account status
		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is not verified. Please check your email.");
		}

		if (user.isBanned()) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is banned. Please contact the administrator.");
		}

		if (Boolean.TRUE.equals(user.getIsSuspended())) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is suspended. Please contact the administrator.");
		}

		if (user.getLoginAttempts() != null && user.getLoginAttempts() >= 5) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is locked due to too many failed login attempts.");
		}

		// Reset login attempts on successful authentication
		userService.resetLoginAttempts(email);

		userService.updateUserLastLoginDate(email);

		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}

	private void handleFailedLoginAttempt(String email) {
		try {
			userService.updateUserLoginAttemptsNumber(email);
		} catch (Exception e) {
			// Log error but don't fail authentication
			System.out.println("Failed to update login attempts for email: " + email + "with error: " + e);
		}
	}

}
