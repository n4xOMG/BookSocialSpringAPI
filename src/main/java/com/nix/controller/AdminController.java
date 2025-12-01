package com.nix.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.AuthorPayoutDTO;
import com.nix.dtos.ContentAnalyticsDTO;
import com.nix.dtos.PlatformAnalyticsDTO;
import com.nix.dtos.RevenueAnalyticsDTO;
import com.nix.dtos.UserAnalyticsDTO;
import com.nix.dtos.UserDTO;
import com.nix.dtos.mappers.UserMapper;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.AdminService;
import com.nix.service.AuthorService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminController {
	@Autowired
	UserService userService;

	@Autowired
	AuthorService authorService;

	@Autowired
	AdminService adminService;

	@Autowired
	UserMapper userMapper;

	@GetMapping("/dashboard/users")
	public ResponseEntity<ApiResponseWithData<UserAnalyticsDTO>> getUserAnalytics() {
		try {
			UserAnalyticsDTO userAnalytics = adminService.getUserAnalytics();
			return buildSuccessResponse("User analytics retrieved successfully.", userAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve user analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/revenue")
	public ResponseEntity<ApiResponseWithData<RevenueAnalyticsDTO>> getRevenueAnalytics() {
		try {

			RevenueAnalyticsDTO revenueAnalytics = adminService.getRevenueAnalytics();
			return buildSuccessResponse("Revenue analytics retrieved successfully.", revenueAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve revenue analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/content")
	public ResponseEntity<ApiResponseWithData<ContentAnalyticsDTO>> getContentAnalytics() {
		try {
			ContentAnalyticsDTO contentAnalytics = adminService.getContentAnalytics();
			return buildSuccessResponse("Content analytics retrieved successfully.", contentAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve content analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/platform")
	public ResponseEntity<ApiResponseWithData<PlatformAnalyticsDTO>> getPlatformAnalytics() {
		try {

			PlatformAnalyticsDTO platformAnalytics = adminService.getPlatformAnalytics();
			return buildSuccessResponse("Platform analytics retrieved successfully.", platformAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve platform analytics: " + e.getMessage());
		}
	}

	@GetMapping("/users")
	public ResponseEntity<ApiResponseWithData<List<UserDTO>>> getAllUsers(
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String searchTerm) {

		try {
			Page<User> usersPage = userService.getAllUsers(page, size, searchTerm);
			List<UserDTO> users = userMapper.mapToDTOs(usersPage.getContent());
			return buildSuccessResponse("Users retrieved successfully.", users);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve users: " + e.getMessage());
		}
	}

	// ===== Payouts (Admin) =====
	@GetMapping("/payouts")
	public ResponseEntity<ApiResponseWithData<Page<AuthorPayoutDTO>>> listPayouts(
			@RequestParam(required = false) com.nix.models.AuthorPayout.PayoutStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "requestedDate,desc") String sort) {
		try {
			String[] parts = sort.split(",");
			String sortField = parts.length > 0 ? parts[0] : "requestedDate";
			String sortDir = parts.length > 1 ? parts[1] : "desc";
			Sort sortSpec = Sort.by(sortField);
			sortSpec = "asc".equalsIgnoreCase(sortDir) ? sortSpec.ascending() : sortSpec.descending();
			Pageable pageable = PageRequest.of(page, size, sortSpec);

			Page<AuthorPayoutDTO> payouts = authorService.listPayouts(status, pageable);
			return buildSuccessResponse("Payouts retrieved successfully.", payouts);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to list payouts: " + e.getMessage());
		}
	}

	@PostMapping("/payouts/{payoutId}/process")
	public ResponseEntity<ApiResponseWithData<AuthorPayoutDTO>> processPayout(@PathVariable UUID payoutId) {
		try {
			AuthorPayoutDTO updated = authorService.processPayout(payoutId);
			return buildSuccessResponse("Payout processed successfully.", updated);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Failed to process payout: " + e.getMessage());
		}
	}

	@GetMapping("/users/total")
	public ResponseEntity<ApiResponseWithData<Long>> getTotalUsers() {
		return buildSuccessResponse("Total users retrieved successfully.", userService.getTotalUsers());
	}

	// Get banned users count
	@GetMapping("/users/banned")
	public ResponseEntity<ApiResponseWithData<Long>> getBannedUsersCount() {
		return buildSuccessResponse("Banned users count retrieved successfully.", userService.getBannedUsersCount());
	}

	// Get suspended users count
	@GetMapping("/users/suspended")
	public ResponseEntity<ApiResponseWithData<Long>> getSuspendedUsersCount() {
		return buildSuccessResponse("Suspended users count retrieved successfully.",
				userService.getSuspendedUsersCount());
	}

	@PutMapping("/users/update/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> updateUser(@PathVariable UUID userId,
			@RequestBody com.nix.dtos.AdminUpdateUserDTO userDTO) throws Exception {
		try {
			UserDTO updateUser = userMapper.mapToDTO(userService.adminUpdateUser(userId, userDTO));

			return buildSuccessResponse("User updated successfully.", updateUser);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to update user: " + e.getMessage());
		}
	}

	@DeleteMapping("/users/delete/{userId}")
	public ResponseEntity<ApiResponseWithData<Void>> deleteUser(@PathVariable UUID userId) throws Exception {
		try {
			userService.deleteUser(userId);
			return buildSuccessResponse("User deleted successfully.", null);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to delete user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/suspend/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> suspendUser(@PathVariable UUID userId) throws Exception {
		try {
			User suspendedUser = userService.suspendUser(userId);
			return buildSuccessResponse("User suspended successfully.", userMapper.mapToDTO(suspendedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to suspend user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/unsuspend/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> unsuspendUser(@PathVariable UUID userId) throws Exception {
		try {
			User unsuspendedUser = userService.unsuspendUser(userId);
			return buildSuccessResponse("User unsuspended successfully.", userMapper.mapToDTO(unsuspendedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to unsuspend user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/ban/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> banUser(@RequestBody Map<String, String> request)
			throws Exception {
		try {
			UUID userId = UUID.fromString(request.get("userId"));
			String banReason = request.get("banReason");

			User bannedUser = userService.banUser(userId, banReason);
			return buildSuccessResponse("User banned successfully.", userMapper.mapToDTO(bannedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to ban user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/ban-batch")
	public ResponseEntity<ApiResponseWithData<List<UserDTO>>> banUsersBatch(@RequestBody Map<String, Object> request) {
		try {
			@SuppressWarnings("unchecked")
			List<String> userIdStrings = (List<String>) request.get("userIds");
			String banReason = (String) request.get("banReason");

			List<UUID> userIds = userIdStrings.stream().map(UUID::fromString).toList();

			List<User> bannedUsers = userService.banUsers(userIds, banReason);
			return buildSuccessResponse("Users banned successfully.", userMapper.mapToDTOs(bannedUsers));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to ban users: " + e.getMessage());
		}
	}

	@PatchMapping("/users/unban/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> unbanUser(@PathVariable UUID userId) throws Exception {
		try {
			User unbannedUser = userService.unbanUser(userId);
			return buildSuccessResponse("User unbanned successfully.", userMapper.mapToDTO(unbannedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to unban user: " + e.getMessage());
		}
	}

	@PutMapping("/users/{id}/role")
	public ResponseEntity<ApiResponseWithData<UserDTO>> updateUserRole(@PathVariable("id") UUID userId,
			@RequestParam String roleName) {
		try {
			User updatedUser = userService.updateUserRole(userId, roleName);
			return buildSuccessResponse("User role updated successfully.", userMapper.mapToDTO(updatedUser));
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST,
					"Failed to update user role: " + e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}

}
