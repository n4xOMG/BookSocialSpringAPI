package com.nix.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.ContentAnalyticsDTO;
import com.nix.dtos.PlatformAnalyticsDTO;
import com.nix.dtos.RevenueAnalyticsDTO;
import com.nix.dtos.UserAnalyticsDTO;
import com.nix.dtos.UserDTO;
import com.nix.models.AuthorPayout;
import com.nix.models.Role;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.AdminService;
import com.nix.service.AuthorService;
import com.nix.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthorService authorService;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getUserAnalytics_returnsAnalytics() {
        String jwt = "Bearer admin-token";
        UserAnalyticsDTO analytics = new UserAnalyticsDTO();
        analytics.setTotalUsers(1000L);
        analytics.setActiveUsers(850L);
        analytics.setBannedUsers(10L);
        analytics.setSuspendedUsers(5L);

        when(adminService.getUserAnalytics()).thenReturn(analytics);

        ResponseEntity<ApiResponseWithData<UserAnalyticsDTO>> response = adminController.getUserAnalytics(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserAnalyticsDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User analytics retrieved successfully.", body.getMessage());
        assertEquals(1000L, body.getData().getTotalUsers());
        assertEquals(850L, body.getData().getActiveUsers());
        assertEquals(10L, body.getData().getBannedUsers());
        assertEquals(5L, body.getData().getSuspendedUsers());

        verify(adminService).getUserAnalytics();
    }

    @Test
    void getRevenueAnalytics_returnsAnalytics() {
        String jwt = "Bearer admin-token";
        RevenueAnalyticsDTO analytics = new RevenueAnalyticsDTO();
        analytics.setTotalRevenue(new BigDecimal("50000.00"));
        analytics.setMonthlyRevenue(new BigDecimal("12000.00"));
        analytics.setWeeklyRevenue(new BigDecimal("3000.00"));
        analytics.setDailyRevenue(new BigDecimal("500.00"));
        analytics.setAverageOrderValue(new BigDecimal("15.00"));
        analytics.setTotalTransactions(3500L);

        when(adminService.getRevenueAnalytics()).thenReturn(analytics);

        ResponseEntity<ApiResponseWithData<RevenueAnalyticsDTO>> response = adminController.getRevenueAnalytics(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<RevenueAnalyticsDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Revenue analytics retrieved successfully.", body.getMessage());
        assertEquals(new BigDecimal("50000.00"), body.getData().getTotalRevenue());
        assertEquals(new BigDecimal("12000.00"), body.getData().getMonthlyRevenue());
        assertEquals(new BigDecimal("3000.00"), body.getData().getWeeklyRevenue());
        assertEquals(new BigDecimal("500.00"), body.getData().getDailyRevenue());
        assertEquals(new BigDecimal("15.00"), body.getData().getAverageOrderValue());
        assertEquals(3500L, body.getData().getTotalTransactions());
    }

    @Test
    void getContentAnalytics_returnsAnalytics() {
        String jwt = "Bearer admin-token";
        ContentAnalyticsDTO analytics = new ContentAnalyticsDTO();
        analytics.setTotalBooks(500L);
        analytics.setTotalChapters(5000L);
        analytics.setTotalUnlocks(10000L);

        when(adminService.getContentAnalytics()).thenReturn(analytics);

        ResponseEntity<ApiResponseWithData<ContentAnalyticsDTO>> response = adminController.getContentAnalytics(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<ContentAnalyticsDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Content analytics retrieved successfully.", body.getMessage());
        assertEquals(500L, body.getData().getTotalBooks());
        assertEquals(5000L, body.getData().getTotalChapters());
        assertEquals(10000L, body.getData().getTotalUnlocks());
    }

    @Test
    void getPlatformAnalytics_returnsAnalytics() {
        String jwt = "Bearer admin-token";
        PlatformAnalyticsDTO analytics = new PlatformAnalyticsDTO();
        analytics.setPlatformFeeEarnings(new BigDecimal("2500.00"));
        analytics.setAuthorEarnings(new BigDecimal("20000.00"));
        analytics.setTotalPayouts(325L);

        when(adminService.getPlatformAnalytics()).thenReturn(analytics);

        ResponseEntity<ApiResponseWithData<PlatformAnalyticsDTO>> response = adminController.getPlatformAnalytics(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<PlatformAnalyticsDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Platform analytics retrieved successfully.", body.getMessage());
        assertEquals(new BigDecimal("2500.00"), body.getData().getPlatformFeeEarnings());
        assertEquals(new BigDecimal("20000.00"), body.getData().getAuthorEarnings());
        assertEquals(325L, body.getData().getTotalPayouts());
    }

    @Test
    void getAllUsers_withSearchTerm_returnsFilteredUsers() {
        String jwt = "Bearer admin-token";
        Page<User> usersPage = new PageImpl<>(Collections.singletonList(buildUser()));

        when(userService.getAllUsers(0, 10, "test")).thenReturn(usersPage);

        ResponseEntity<ApiResponseWithData<List<UserDTO>>> response = adminController.getAllUsers(jwt, 0, 10,
                "test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<List<UserDTO>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Users retrieved successfully.", body.getMessage());
        assertEquals(1, body.getData().size());

        verify(userService).getAllUsers(0, 10, "test");
    }

    @Test
    void getAllUsers_withoutSearchTerm_returnsAllUsers() {
        String jwt = "Bearer admin-token";
        Page<User> usersPage = new PageImpl<>(Collections.singletonList(buildUser()));

        when(userService.getAllUsers(0, 10, null)).thenReturn(usersPage);

        ResponseEntity<ApiResponseWithData<List<UserDTO>>> response = adminController.getAllUsers(jwt, 0, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<List<UserDTO>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());

        verify(userService).getAllUsers(0, 10, null);
    }

    @Test
    void listPayouts_withStatus_returnsFilteredPayouts() {
        String jwt = "Bearer admin-token";
        User admin = buildAdmin();
        AuthorPayoutDTO payoutDTO = buildPayoutDTO();
        Page<AuthorPayoutDTO> payoutsPage = new PageImpl<>(Collections.singletonList(payoutDTO));

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(authorService.listPayouts(eq(AuthorPayout.PayoutStatus.PENDING), any(Pageable.class)))
                .thenReturn(payoutsPage);

        ResponseEntity<ApiResponseWithData<Page<AuthorPayoutDTO>>> response = adminController.listPayouts(jwt,
                AuthorPayout.PayoutStatus.PENDING, 0, 20, "requestedDate,desc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<Page<AuthorPayoutDTO>> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Payouts retrieved successfully.", body.getMessage());
        assertEquals(AuthorPayout.PayoutStatus.PENDING, body.getData().getContent().get(0).getStatus());

        verify(authorService).listPayouts(eq(AuthorPayout.PayoutStatus.PENDING), any(Pageable.class));
    }

    @Test
    void listPayouts_withoutAuth_returnsUnauthorized() {
        String invalidJwt = "Bearer invalid-token";
        when(userService.findUserByJwt(invalidJwt)).thenReturn(null);

        ResponseEntity<ApiResponseWithData<Page<AuthorPayoutDTO>>> response = adminController.listPayouts(invalidJwt,
                AuthorPayout.PayoutStatus.PENDING, 0, 20, "requestedDate,desc");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponseWithData<Page<AuthorPayoutDTO>> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Authentication is required to manage payouts.", body.getMessage());
    }

    @Test
    void processPayout_withValidId_processesSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID payoutId = UUID.randomUUID();
        User admin = buildAdmin();
        AuthorPayoutDTO payoutDTO = buildPayoutDTO();
        payoutDTO.setStatus(AuthorPayout.PayoutStatus.COMPLETED);

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(authorService.processPayout(payoutId)).thenReturn(payoutDTO);

        ResponseEntity<ApiResponseWithData<AuthorPayoutDTO>> response = adminController.processPayout(jwt, payoutId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<AuthorPayoutDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Payout processed successfully.", body.getMessage());
        assertEquals(AuthorPayout.PayoutStatus.COMPLETED, body.getData().getStatus());

        verify(authorService).processPayout(payoutId);
    }

    @Test
    void processPayout_withoutAuth_returnsUnauthorized() {
        UUID payoutId = UUID.randomUUID();
        String invalidJwt = "Bearer invalid-token";
        when(userService.findUserByJwt(invalidJwt)).thenReturn(null);

        ResponseEntity<ApiResponseWithData<AuthorPayoutDTO>> response = adminController.processPayout(invalidJwt,
                payoutId);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponseWithData<AuthorPayoutDTO> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Authentication is required to process payouts.", body.getMessage());
    }

    @Test
    void getTotalUsers_returnsCount() {
        String jwt = "Bearer admin-token";
        User admin = buildAdmin();

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.getTotalUsers()).thenReturn(1500L);

        ResponseEntity<ApiResponseWithData<Long>> response = adminController.getTotalUsers(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<Long> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Total users retrieved successfully.", body.getMessage());
        assertEquals(1500L, body.getData());
    }

    @Test
    void getBannedUsersCount_returnsCount() {
        String jwt = "Bearer admin-token";
        User admin = buildAdmin();

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.getBannedUsersCount()).thenReturn(25L);

        ResponseEntity<ApiResponseWithData<Long>> response = adminController.getBannedUsersCount(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<Long> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Banned users count retrieved successfully.", body.getMessage());
        assertEquals(25L, body.getData());
    }

    @Test
    void getSuspendedUsersCount_returnsCount() {
        String jwt = "Bearer admin-token";
        User admin = buildAdmin();

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.getSuspendedUsersCount()).thenReturn(15L);

        ResponseEntity<ApiResponseWithData<Long>> response = adminController.getSuspendedUsersCount(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<Long> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Suspended users count retrieved successfully.", body.getMessage());
        assertEquals(15L, body.getData());
    }

    @Test
    void updateUser_withValidData_updatesSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = buildUser();
        user.setId(userId);

        when(userService.updateUser(eq(userId), any(User.class))).thenReturn(user);

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.updateUser(userId, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User updated successfully.", body.getMessage());
        assertEquals(userId, body.getData().getId());

        verify(userService).updateUser(eq(userId), any(User.class));
    }

    @Test
    void deleteUser_withAuth_deletesSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID userId = UUID.randomUUID();
        User admin = buildAdmin();

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.deleteUser(userId)).thenReturn("User deleted successfully");

        ResponseEntity<ApiResponseWithData<Void>> response = adminController.deleteUser(jwt, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<Void> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User deleted successfully.", body.getMessage());
        assertNull(body.getData());

        verify(userService).deleteUser(userId);
    }

    @Test
    void deleteUser_withoutAuth_returnsUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();
        String invalidJwt = "Bearer invalid-token";
        when(userService.findUserByJwt(invalidJwt)).thenReturn(null);

        ResponseEntity<ApiResponseWithData<Void>> response = adminController.deleteUser(invalidJwt, userId);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ApiResponseWithData<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Authentication is required to delete users.", body.getMessage());
    }

    @Test
    void suspendUser_withAuth_suspendsSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID userId = UUID.randomUUID();
        User admin = buildAdmin();
        User suspendedUser = buildUser();
        suspendedUser.setIsSuspended(true);

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.suspendUser(userId)).thenReturn(suspendedUser);

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.suspendUser(jwt, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User suspended successfully.", body.getMessage());
        assertTrue(Boolean.TRUE.equals(body.getData().getIsSuspended()));

        verify(userService).suspendUser(userId);
    }

    @Test
    void unsuspendUser_withAuth_unsuspendsSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID userId = UUID.randomUUID();
        User admin = buildAdmin();
        User unsuspendedUser = buildUser();
        unsuspendedUser.setIsSuspended(false);

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.unsuspendUser(userId)).thenReturn(unsuspendedUser);

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.unsuspendUser(jwt, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User unsuspended successfully.", body.getMessage());
        assertFalse(Boolean.TRUE.equals(body.getData().getIsSuspended()));

        verify(userService).unsuspendUser(userId);
    }

    @Test
    void banUser_withReason_bansSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID userId = UUID.randomUUID();
        User admin = buildAdmin();
        User bannedUser = buildUser();
        bannedUser.setBanned(true);
        bannedUser.setBanReason("Violation of terms");

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.banUser(userId, "Violation of terms")).thenReturn(bannedUser);

        Map<String, String> request = Map.of(
                "userId", userId.toString(),
                "banReason", "Violation of terms");

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.banUser(jwt, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User banned successfully.", body.getMessage());
        assertTrue(body.getData().isBanned());
        assertEquals("Violation of terms", body.getData().getBanReason());

        verify(userService).banUser(userId, "Violation of terms");
    }

    @Test
    void unbanUser_withAuth_unbansSuccessfully() throws Exception {
        String jwt = "Bearer admin-token";
        UUID userId = UUID.randomUUID();
        User admin = buildAdmin();
        User unbannedUser = buildUser();
        unbannedUser.setBanned(false);

        when(userService.findUserByJwt(jwt)).thenReturn(admin);
        when(userService.unbanUser(userId)).thenReturn(unbannedUser);

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.unbanUser(jwt, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User unbanned successfully.", body.getMessage());
        assertFalse(body.getData().isBanned());

        verify(userService).unbanUser(userId);
    }

    @Test
    void updateUserRole_toModerator_updatesSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = buildUser();
        Role moderatorRole = new Role();
        moderatorRole.setName("MODERATOR");
        user.setRole(moderatorRole);

        when(userService.updateUserRole(userId, "MODERATOR")).thenReturn(user);

        ResponseEntity<ApiResponseWithData<UserDTO>> response = adminController.updateUserRole(userId, "MODERATOR");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponseWithData<UserDTO> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("User role updated successfully.", body.getMessage());
        assertEquals("MODERATOR", body.getData().getRole().getName());

        verify(userService).updateUserRole(userId, "MODERATOR");
    }

    private User buildAdmin() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@example.com");
        admin.setIsVerified(true);
        admin.setBanned(false);
        admin.setIsSuspended(false);
        Role role = new Role();
        role.setName("ADMIN");
        admin.setRole(role);
        return admin;
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setUsername("testuser");
        user.setIsVerified(true);
        user.setBanned(false);
        user.setIsSuspended(false);
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        return user;
    }

    private AuthorPayoutDTO buildPayoutDTO() {
        AuthorPayoutDTO dto = new AuthorPayoutDTO();
        dto.setId(UUID.randomUUID());
        dto.setTotalAmount(new BigDecimal("100.00"));
        dto.setStatus(AuthorPayout.PayoutStatus.PENDING);
        dto.setRequestedDate(LocalDateTime.now());
        return dto;
    }
}
