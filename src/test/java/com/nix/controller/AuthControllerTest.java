package com.nix.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nix.config.JwtProvider;
import com.nix.exception.AccountException;
import com.nix.models.Role;
import com.nix.models.User;
import com.nix.request.LoginRequest;
import com.nix.service.CustomUserDetailsService;
import com.nix.service.UserService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void signUp_withNewEmail_createsUserAndReturnsToken() throws Exception {
        User newUser = buildUser("newuser@example.com", "password123");

        when(userService.findUserByEmail("newuser@example.com")).thenReturn(null);
        when(userService.register(any(User.class))).thenReturn(newUser);

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sign up succeeded!"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());

        verify(userService).register(any(User.class));
    }

    @Test
    void signUp_withExistingEmail_returnsNotAcceptable() throws Exception {
        User existingUser = buildUser("existing@example.com", "password123");
        User newUser = buildUser("existing@example.com", "password456");

        when(userService.findUserByEmail("existing@example.com")).thenReturn(existingUser);

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.message").value("Email already exists!"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        User user = buildVerifiedUser("user@example.com", "encodedPassword");
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("user@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login succeeded!"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());

        verify(userService).resetLoginAttempts("user@example.com");
        verify(userService).updateUserLastLoginDate("user@example.com");
    }

    @Test
    void login_withInvalidPassword_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("wrongpassword");
        loginRequest.setRememberMe(false);

        User user = buildVerifiedUser("user@example.com", "encodedPassword");
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid email/password!"))
                .andExpect(jsonPath("$.success").value(false));

        verify(userService).updateUserLoginAttemptsNumber("user@example.com");
    }

    @Test
    void login_withUnverifiedAccount_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unverified@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        User user = buildUser("unverified@example.com", "encodedPassword");
        user.setIsVerified(false);
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("unverified@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("unverified@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is not verified. Please check your email."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withBannedAccount_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("banned@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        User user = buildVerifiedUser("banned@example.com", "encodedPassword");
        user.setBanned(true);
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("banned@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("banned@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is banned. Please contact the administrator."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withSuspendedAccount_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("suspended@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        User user = buildVerifiedUser("suspended@example.com", "encodedPassword");
        user.setIsSuspended(true);
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("suspended@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("suspended@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is suspended. Please contact the administrator."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_withLockedAccount_returnsForbidden() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("locked@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        User user = buildVerifiedUser("locked@example.com", "encodedPassword");
        user.setLoginAttempts(5);
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("locked@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("locked@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is locked due to too many failed login attempts."))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void forgotPassword_withValidEmail_sendsResetLink() throws Exception {
        User user = buildVerifiedUser("user@example.com", "password");
        Map<String, String> request = Map.of("email", "user@example.com");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);
        when(userService.sendForgotPasswordMail(user)).thenReturn(user);

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset password link has been sent to your email."))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).sendForgotPasswordMail(user);
    }

    @Test
    void forgotPassword_withInvalidEmail_returnsBadRequest() throws Exception {
        Map<String, String> request = Map.of("email", "nonexistent@example.com");

        when(userService.findUserByEmail("nonexistent@example.com")).thenReturn(null);

        mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email not found!"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resetPassword_withValidData_resetsPassword() throws Exception {
        User user = buildVerifiedUser("user@example.com", "oldpassword");
        Map<String, String> request = Map.of("email", "user@example.com", "password", "newpassword");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);
        when(userService.updateUserPassword("newpassword", user)).thenReturn(user);

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully."))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).updateUserPassword("newpassword", user);
    }

    @Test
    void resetPassword_withInvalidEmail_returnsBadRequest() throws Exception {
        Map<String, String> request = Map.of("email", "nonexistent@example.com", "password", "newpassword");

        when(userService.findUserByEmail("nonexistent@example.com")).thenReturn(null);

        mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found!"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyOtp_withValidOtp_verifiesUser() throws Exception {
        User user = buildUser("user@example.com", "password");
        user.setVerificationCode("123456");
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "otp", "123456",
                "context", "REGISTER");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);
        when(userService.updateUser(eq(user.getId()), any(User.class))).thenReturn(user);

        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully. Registration complete."))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).updateUser(eq(user.getId()), any(User.class));
    }

    @Test
    void verifyOtp_withInvalidOtp_returnsBadRequest() throws Exception {
        User user = buildUser("user@example.com", "password");
        user.setVerificationCode("123456");
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "otp", "654321",
                "context", "REGISTER");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP!"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyOtp_forResetPassword_returnsCorrectMessage() throws Exception {
        User user = buildUser("user@example.com", "password");
        user.setVerificationCode("123456");
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "otp", "123456",
                "context", "RESET_PASSWORD");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);
        when(userService.updateUser(eq(user.getId()), any(User.class))).thenReturn(user);

        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully. Please reset your password."))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void verifyOtp_withInvalidContext_returnsBadRequest() throws Exception {
        User user = buildUser("user@example.com", "password");
        user.setVerificationCode("123456");
        Map<String, String> request = Map.of(
                "email", "user@example.com",
                "otp", "123456",
                "context", "INVALID_CONTEXT");

        when(userService.findUserByEmail("user@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid context!"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateUserEmail_withValidJwt_updatesEmail() throws Exception {
        String jwt = "Bearer test-token";
        User user = buildVerifiedUser("old@example.com", "password");
        Map<String, String> request = Map.of("email", "new@example.com");

        when(userService.findUserByJwt(jwt)).thenReturn(user);
        when(userService.updateUser(eq(user.getId()), any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/user/update-email")
                .header("Authorization", jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully."))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).updateUser(eq(user.getId()), any(User.class));
    }

    @Test
    void login_withRememberMe_generatesLongerToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(true);

        User user = buildVerifiedUser("user@example.com", "encodedPassword");
        UserDetails userDetails = buildUserDetails(user);

        when(customUserDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userService.findUserByEmail("user@example.com")).thenReturn(user);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login succeeded!"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());

        verify(userService).resetLoginAttempts("user@example.com");
        verify(userService).updateUserLastLoginDate("user@example.com");
    }

    private User buildUser(String email, String password) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword(password);
        user.setUsername("testuser");
        user.setIsVerified(false);
        user.setBanned(false);
        user.setIsSuspended(false);
        user.setLoginAttempts(0);
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        return user;
    }

    private User buildVerifiedUser(String email, String password) {
        User user = buildUser(email, password);
        user.setIsVerified(true);
        return user;
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().getName())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getIsVerified())
                .build();
    }
}
