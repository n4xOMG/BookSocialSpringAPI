package com.nix.controller;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.nix.enums.VerificationContext;
import com.nix.exception.AccountException;
import com.nix.models.User;
import com.nix.request.LoginRequest;
import com.nix.response.ApiResponseWithData;
import com.nix.service.CustomUserDetailsService;
import com.nix.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AuthController {
	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
	@Autowired
	private PasswordEncoder passEncoder;

	@Autowired
	private UserService userService;

	@Autowired
	private CustomUserDetailsService customUserDetails;

	@Autowired
	private JwtProvider jwtProvider;

	@Value("${frontend.url}")
	private String frontendUrl;

	@PostMapping("/auth/signup")
	public ResponseEntity<ApiResponseWithData<String>> signUp(@RequestBody User user, HttpServletRequest request)
			throws Exception {
		User isExistedUser = userService.findUserByEmail(user.getEmail());
		if (isExistedUser != null) {
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
					.body(new ApiResponseWithData<>("Email already exists!", false));
		}

		User newUser = userService.register(user);
		Authentication auth = new UsernamePasswordAuthenticationToken(newUser.getEmail(), newUser.getPassword());

		String token = jwtProvider.generateToken(auth, false);

		return ResponseEntity.ok(new ApiResponseWithData<>("Sign up succeeded!", true, token));
	}

	@PostMapping("/auth/signin")
	public ResponseEntity<ApiResponseWithData<String>> login(@RequestBody LoginRequest loginReq) {
		boolean rememberMe = loginReq.isRememberMe();
		Authentication auth = null;
		try {
			auth = authenticate(loginReq.getEmail(), loginReq.getPassword());
		} catch (BadCredentialsException e) {
			logger.warn("Authentication failed for email: {}", loginReq.getEmail(), e);
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ApiResponseWithData<>("Invalid email/password!", false));
		} catch (AccountException e) {

			logger.warn("Account exception for email: {}: {}", loginReq.getEmail(), e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}

		if (auth == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ApiResponseWithData<>("Invalid email/password!", false));
		}

		String token = jwtProvider.generateToken(auth, rememberMe);
		userService.updateUserLastLoginDate(loginReq.getEmail());

		return ResponseEntity.ok(new ApiResponseWithData<>("Login succeeded!", true, token));
	}

	@PostMapping("/auth/forgot-password")
	public ResponseEntity<ApiResponseWithData<Void>> sendForgotPasswordMail(@RequestBody Map<String, String> request,
			HttpServletRequest httpRequest) throws UnsupportedEncodingException, MessagingException {
		String email = request.get("email");
		User user = userService.findUserByEmail(email);
		if (user == null) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email not found!");
		}

		userService.sendForgotPasswordMail(user);

		return buildSuccessResponse("Reset password link has been sent to your email.", null);
	}

	@PostMapping("/api/user/update-email")
	public ResponseEntity<ApiResponseWithData<Void>> updateUserEmail(@RequestHeader("Authorization") String jwt,
			@RequestBody Map<String, String> request) {
		User user = userService.findUserByJwt(jwt);

		user.setEmail(request.get("email"));
		user.setIsVerified(true);
		user.setVerificationCode(null);
		userService.updateUser(user.getId(), user);

		return buildSuccessResponse("Email verified successfully.", null);
	}

	@PostMapping("/auth/reset-password")
	public ResponseEntity<ApiResponseWithData<Void>> resetPassword(@RequestBody Map<String, String> request) {
		try {
			User user = userService.findUserByEmail(request.get("email"));
			String password = request.get("password");
			String resetToken = request.get("resetToken");

			if (user == null) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST, "User not found!");
			}

			// Validate password reset token (required after OTP verification)
			if (resetToken == null || resetToken.isBlank()) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST, "Reset token is required.");
			}

			if (!userService.validatePasswordResetToken(user, resetToken)) {
				return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid or expired reset token.");
			}

			userService.resetPasswordWithToken(password, user);

			return buildSuccessResponse("Password has been reset successfully.", null);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to reset password: " + e.getMessage());
		}
	}

	@PostMapping("/auth/verify-otp")
	public ResponseEntity<ApiResponseWithData<String>> verifyOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		String otp = request.get("otp");
		VerificationContext context = VerificationContext.from(request.get("context"));
		User user = userService.findUserByEmail(email);
		if (user == null) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "User not found!");
		}

		// Check if OTP matches
		if (user.getVerificationCode() == null || !otp.equals(user.getVerificationCode())) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid or expired OTP!");
		}

		// Check if OTP has expired
		if (user.getOtpExpiration() == null || LocalDateTime.now().isAfter(user.getOtpExpiration())) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new one.");
		}

		if (context == null) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid context!");
		}

		user.setIsVerified(true);
		user.setVerificationCode(null);
		user.setOtpExpiration(null); // Clear expiration
		userService.updateUser(user.getId(), user);

		return switch (context) {
			case REGISTER -> buildSuccessResponse("OTP verified successfully. Registration complete.", null);
			case RESET_PASSWORD -> {
				// Generate a secure password reset token (valid for 15 minutes)
				String resetToken = userService.generatePasswordResetToken(user);
				yield buildSuccessResponse("OTP verified. Use the token to reset your password.", resetToken);
			}
			case UPDATE_PROFILE -> buildSuccessResponse("OTP verified successfully. Update profile complete.", null);
		};
	}

	public Authentication authenticate(String email, String password) {

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

		User user = userService.findUserByEmail(email);
		if (user == null) {
			handleFailedLoginAttempt(email);
			throw new BadCredentialsException("Invalid email or password");
		}

		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is not verified. Please check your email.");
		}

		// Banned users cannot log in at all
		if (user.isBanned()) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is banned. Please contact the administrator.");
		}

		// Note: Suspended users CAN log in (browse, read content)
		// They are only blocked from content creation activities by controller-level
		// checks

		if (user.getLoginAttempts() != null && user.getLoginAttempts() >= 5) {
			handleFailedLoginAttempt(email);
			throw new AccountException("Account is locked due to too many failed login attempts.");
		}
		userService.resetLoginAttempts(email);

		userService.updateUserLastLoginDate(email);

		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}

	private void handleFailedLoginAttempt(String email) {
		try {
			userService.updateUserLoginAttemptsNumber(email);
		} catch (Exception e) {
			logger.error("Failed to update login attempts for email: {}", email, e);
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}

}
