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

	UserMapper userMapper = new UserMapper();

	@GetMapping("/dashboard/users")
	public ResponseEntity<ApiResponseWithData<UserAnalyticsDTO>> getUserAnalytics(
			@RequestHeader("Authorization") String jwt) {
		try {
			UserAnalyticsDTO userAnalytics = adminService.getUserAnalytics();
			return buildSuccessResponse("User analytics retrieved successfully.", userAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve user analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/revenue")
	public ResponseEntity<ApiResponseWithData<RevenueAnalyticsDTO>> getRevenueAnalytics(
			@RequestHeader("Authorization") String jwt) {
		try {

			RevenueAnalyticsDTO revenueAnalytics = adminService.getRevenueAnalytics();
			return buildSuccessResponse("Revenue analytics retrieved successfully.", revenueAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve revenue analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/content")
	public ResponseEntity<ApiResponseWithData<ContentAnalyticsDTO>> getContentAnalytics(
			@RequestHeader("Authorization") String jwt) {
		try {
			ContentAnalyticsDTO contentAnalytics = adminService.getContentAnalytics();
			return buildSuccessResponse("Content analytics retrieved successfully.", contentAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve content analytics: " + e.getMessage());
		}
	}

	@GetMapping("/dashboard/platform")
	public ResponseEntity<ApiResponseWithData<PlatformAnalyticsDTO>> getPlatformAnalytics(
			@RequestHeader("Authorization") String jwt) {
		try {

			PlatformAnalyticsDTO platformAnalytics = adminService.getPlatformAnalytics();
			return buildSuccessResponse("Platform analytics retrieved successfully.", platformAnalytics);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve platform analytics: " + e.getMessage());
		}
	}

	@GetMapping("/users")
	public ResponseEntity<ApiResponseWithData<List<UserDTO>>> getAllUsers(@RequestHeader("Authorization") String jwt,
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
			@RequestHeader("Authorization") String jwt,
			@RequestParam(required = false) com.nix.models.AuthorPayout.PayoutStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "requestedDate,desc") String sort) {
		try {
			User admin = userService.findUserByJwt(jwt);
			if (admin == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to manage payouts.");
			}

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
	public ResponseEntity<ApiResponseWithData<AuthorPayoutDTO>> processPayout(
			@RequestHeader("Authorization") String jwt, @PathVariable UUID payoutId) {
		try {
			User admin = userService.findUserByJwt(jwt);
			if (admin == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to process payouts.");
			}
			AuthorPayoutDTO updated = authorService.processPayout(payoutId);
			return buildSuccessResponse("Payout processed successfully.", updated);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Failed to process payout: " + e.getMessage());
		}
	}

	@GetMapping("/users/total")
	public ResponseEntity<ApiResponseWithData<Long>> getTotalUsers(@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication is required to view totals.");
		}
		return buildSuccessResponse("Total users retrieved successfully.", userService.getTotalUsers());
	}

	// Get banned users count
	@GetMapping("/users/banned")
	public ResponseEntity<ApiResponseWithData<Long>> getBannedUsersCount(
			@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED,
					"Authentication is required to view banned user metrics.");
		}
		return buildSuccessResponse("Banned users count retrieved successfully.", userService.getBannedUsersCount());
	}

	// Get suspended users count
	@GetMapping("/users/suspended")
	public ResponseEntity<ApiResponseWithData<Long>> getSuspendedUsersCount(
			@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED,
					"Authentication is required to view suspended user metrics.");
		}
		return buildSuccessResponse("Suspended users count retrieved successfully.",
				userService.getSuspendedUsersCount());
	}

	@PutMapping("/users/update/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> updateUser(@PathVariable UUID userId,
			@RequestBody User user) throws Exception {
		try {
			UserDTO updateUser = userMapper.mapToDTO(userService.updateUser(userId, user));

			return buildSuccessResponse("User updated successfully.", updateUser);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to update user: " + e.getMessage());
		}
	}

	@DeleteMapping("/users/delete/{userId}")
	public ResponseEntity<ApiResponseWithData<Void>> deleteUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to delete users.");
			}
			userService.deleteUser(userId);
			return buildSuccessResponse("User deleted successfully.", null);

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to delete user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/suspend/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> suspendUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to suspend users.");
			}
			User suspendedUser = userService.suspendUser(userId);
			return buildSuccessResponse("User suspended successfully.", userMapper.mapToDTO(suspendedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to suspend user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/unsuspend/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> unsuspendUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to unsuspend users.");
			}
			User unsuspendedUser = userService.unsuspendUser(userId);
			return buildSuccessResponse("User unsuspended successfully.", userMapper.mapToDTO(unsuspendedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to unsuspend user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/ban/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> banUser(@RequestHeader("Authorization") String jwt,
			@RequestBody Map<String, String> request) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to ban users.");
			}
			UUID userId = UUID.fromString(request.get("userId"));
			String banReason = request.get("banReason");

			User bannedUser = userService.banUser(userId, banReason);
			return buildSuccessResponse("User banned successfully.", userMapper.mapToDTO(bannedUser));

		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to ban user: " + e.getMessage());
		}
	}

	@PatchMapping("/users/unban/{userId}")
	public ResponseEntity<ApiResponseWithData<UserDTO>> unbanUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED,
						"Authentication is required to unban users.");
			}
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
