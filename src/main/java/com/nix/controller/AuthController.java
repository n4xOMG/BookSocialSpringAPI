package com.nix.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
import jakarta.servlet.http.HttpServletResponse;

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

		User newUser = userService.register(user, getSiteURL(request));
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
	        System.out.println("Account not verified: " + e.getMessage());
	        AuthResponse authRes = new AuthResponse(null, e.getMessage());
	        return new ResponseEntity<>(authRes, HttpStatus.FORBIDDEN);
	    }

	    if (auth == null) {
	        AuthResponse authRes = new AuthResponse(null, "Invalid email/password!");
	        return new ResponseEntity<>(authRes, HttpStatus.UNAUTHORIZED);
	    }

	    String token = JwtProvider.generateToken(auth, rememberMe);
	    AuthResponse authRes = new AuthResponse(token, "Login succeeded!");
	    return ResponseEntity.ok(authRes);
	}

	@PostMapping("/auth/forgot-password")
	public ResponseEntity<?> semdForgotPasswordMail(@RequestBody Map<String, String> request,
			HttpServletRequest httpRequest) throws UnsupportedEncodingException, MessagingException {
		String email = request.get("email");
		User user = userService.findUserByEmail(email);
		if (user == null) {
			return ResponseEntity.badRequest().body("Email not found!");
		}

		userService.sendForgotPasswordMail(user, getSiteURL(httpRequest));

		return ResponseEntity.ok("Reset password link has been sent to your email.");
	}

	@GetMapping("/auth/reset-password")
	public void redirectToResetPassword(@Param("code") String code, HttpServletResponse response) throws IOException {
		User user = userService.verifyUser(code);

		if (user == null) {
			return;
		}

		response.sendRedirect(frontendUrl + "/reset-password?code=" + code);

	}

	@PostMapping("/auth/reset-password")
	public ResponseEntity<?> resetPassword(@Param("code") String code, @RequestBody Map<String, String> request) {
		try {
			User user = userService.verifyUser(code);
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

	@GetMapping("/auth/verify")
	public ResponseEntity<String> verifyEmail(@RequestParam("code") String code, @RequestParam("action") String action,
			@RequestParam("email") String email) throws Exception {
		User user = userService.verifyUser(code);

		if (user == null) {
			return new ResponseEntity<>("Invalid verification code", HttpStatus.BAD_REQUEST);
		}

		user.setIsVerified(true);

		String redirectUrl;
		if ("updateProfile".equals(action)) {
			user.setVerificationCode(null);
			user.setEmail(email);
			redirectUrl = frontendUrl + "/sign-in";
		} else if ("register".equals(action)) {
			user.setVerificationCode(null);
			redirectUrl = frontendUrl + "/sign-in";
		} else if ("reset-password".equals(action)) {
			redirectUrl = frontendUrl + "/reset-password?code=" + code;
		} else {
			redirectUrl = frontendUrl + "/";
		}

		String htmlResponse = "<html>" + "<head>" + "<meta http-equiv='refresh' content='5;url=" + redirectUrl + "' />"
				+ "<script>" + "setTimeout(function() {" + "  window.location.href = '" + redirectUrl + "';"
				+ "}, 5000);" + "</script>" + "</head>" + "<body>"
				+ "<p>Email verified successfully. You will be redirected in 5 seconds...</p>" + "</body>" + "</html>";
		userService.updateUser(user.getId(), user);
		return ResponseEntity.status(HttpStatus.OK).body(htmlResponse);
	}

	public Authentication authenticate(String email, String password) {
		UserDetails userDetails = customUserDetails.loadUserByUsername(email);
		User user = userService.findUserByEmail(email);
		if (userDetails == null) {
			throw new BadCredentialsException("No email found with: " + email);
		}
		if (!passEncoder.matches(password, userDetails.getPassword())) {
			throw new BadCredentialsException("Invalid Password!");
		}
		if (user.getIsVerified() == false || user.getIsVerified() == null) {
			throw new AccountException("Account is not verified, please check your mail!");
		}
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

	}

	private String getSiteURL(HttpServletRequest request) {
		String siteURL = request.getRequestURL().toString();
		return siteURL.replace(request.getServletPath(), "");
	}
}
