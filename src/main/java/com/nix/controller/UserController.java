package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.UserDTO;
import com.nix.dtos.UserSummaryDTO;
import com.nix.dtos.mappers.UserMapper;
import com.nix.dtos.mappers.UserSummaryMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.UserService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class UserController {
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	UserService userService;

	UserMapper userMapper = new UserMapper();

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	UserSummaryMapper userSummaryDTO = new UserSummaryMapper();

	@GetMapping("/user/profile/{userId}")
	public ResponseEntity<ApiResponseWithData<UserSummaryDTO>> getUserProfile(@PathVariable("userId") UUID userId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			User otherUser = userService.findUserById(userId);
			UserSummaryDTO userSummaryDTO = userSummaryMapper.mapToDTO(otherUser);

			if (jwt != null && !jwt.isEmpty()) {
				User currentUser = userService.findUserByJwt(jwt);
				if (userService.isBlockedBy(currentUser.getId(), otherUser.getId())) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body(new ApiResponseWithData<>("You are not allowed to view this profile.", false));
				}

				boolean isFollowing = userService.isFollowedByCurrentUser(currentUser, otherUser);
				userSummaryDTO.setFollowedByCurrentUser(isFollowing);
			}

			return ResponseEntity
					.ok(new ApiResponseWithData<>("User profile retrieved successfully.", true, userSummaryDTO));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@GetMapping("/api/user/profile")
	public ResponseEntity<ApiResponseWithData<UserDTO>> getUserFromToken(@RequestHeader("Authorization") String jwt) {
		logger.debug("Getting user profile from JWT token");
		try {
			User user = userService.findUserByJwt(jwt);
			UserDTO userDTO = userMapper.mapToDTO(user);
			return ResponseEntity.ok(new ApiResponseWithData<>("User profile retrieved successfully.", true, userDTO));
		} catch (JwtException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ApiResponseWithData<>("Invalid or expired JWT token", false));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	private String getSiteURL(HttpServletRequest request) {
		String siteURL = request.getRequestURL().toString();
		return siteURL.replace(request.getServletPath(), "");
	}

	@PutMapping("/api/user/profile")
	public ResponseEntity<ApiResponseWithData<UserDTO>> updateProfile(@RequestHeader("Authorization") String jwt,
			@Valid @RequestBody User user,
			HttpServletRequest httpRequest) throws Exception {

		try {
			UserDTO updateUser = userMapper
					.mapToDTO(userService.updateCurrentSessionUser(jwt, user, getSiteURL(httpRequest)));
			logger.info("Updated profile for user: {}", updateUser.getEmail());

			// Check if there's a pending email change
			User currentUser = userService.findUserByJwt(jwt);
			String message = currentUser.getPendingEmail() != null
					? "Profile updated. Please check your new email for verification OTP."
					: "Profile updated successfully.";

			return ResponseEntity.ok(new ApiResponseWithData<>(message, true, updateUser));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PostMapping("/api/user/confirm-email-change")
	public ResponseEntity<ApiResponseWithData<UserDTO>> confirmEmailChange(
			@RequestHeader("Authorization") String jwt,
			@RequestBody java.util.Map<String, String> request) {
		try {
			String otp = request.get("otp");
			if (otp == null || otp.isEmpty()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new ApiResponseWithData<>("OTP is required.", false));
			}

			User updatedUser = userService.confirmEmailChange(jwt, otp);
			UserDTO userDTO = userMapper.mapToDTO(updatedUser);
			logger.info("Email change confirmed for user: {}", updatedUser.getId());

			return ResponseEntity.ok(new ApiResponseWithData<>(
					"Email changed successfully. A recovery email has been sent to your previous address.",
					true, userDTO));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		} catch (Exception e) {
			logger.error("Error confirming email change", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PostMapping("/user/rollback-email")
	public ResponseEntity<ApiResponseWithData<UserDTO>> rollbackEmail(@RequestParam String token) {
		try {
			User updatedUser = userService.rollbackEmail(token);
			UserDTO userDTO = userMapper.mapToDTO(updatedUser);
			logger.info("Email rolled back for user: {}", updatedUser.getId());

			return ResponseEntity.ok(new ApiResponseWithData<>(
					"Email has been successfully restored to your previous address.",
					true, userDTO));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		} catch (Exception e) {
			logger.error("Error rolling back email", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@GetMapping("/api/user/search")
	public ResponseEntity<ApiResponseWithData<List<UserDTO>>> searchUser(@RequestParam("query") String query)
			throws Exception {

		try {
			List<UserDTO> foundUsers = userMapper.mapToDTOs(userService.findUserByUsername(query));
			return ResponseEntity.ok(new ApiResponseWithData<>("Users found successfully.", true, foundUsers));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PostMapping("/api/user/follow/{userIdToFollow}")
	public ResponseEntity<ApiResponseWithData<UserSummaryDTO>> followUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userIdToFollow) {
		try {
			UserSummaryDTO userSummaryDTO = new UserSummaryDTO();
			User currentUser = userService.findUserByJwt(jwt);
			User updatedUser = userService.followUser(currentUser.getId(), userIdToFollow);
			if (updatedUser != null) {
				userSummaryDTO = userSummaryMapper.mapToDTO(updatedUser);
				userSummaryDTO.setFollowedByCurrentUser(true);
			} else {
				userSummaryDTO.setFollowedByCurrentUser(false);
			}
			String message = updatedUser != null ? "User followed successfully." : "Failed to follow user.";
			return ResponseEntity.ok(new ApiResponseWithData<>(message, true, userSummaryDTO));
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		}
	}

	@PostMapping("/api/user/unfollow/{userIdToUnfollow}")
	public ResponseEntity<ApiResponseWithData<UserSummaryDTO>> unFollowUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userIdToUnfollow) {
		try {
			UserSummaryDTO userSummaryDTO = new UserSummaryDTO();
			User currentUser = userService.findUserByJwt(jwt);
			User updatedUser = userService.unFollowUser(currentUser.getId(), userIdToUnfollow);
			if (updatedUser != null) {
				userSummaryDTO = userSummaryMapper.mapToDTO(updatedUser);
				userSummaryDTO.setFollowedByCurrentUser(false);
			} else {
				userSummaryDTO.setFollowedByCurrentUser(true);
			}
			String message = updatedUser != null ? "User unfollowed successfully." : "Failed to unfollow user.";
			return ResponseEntity.ok(new ApiResponseWithData<>(message, true, userSummaryDTO));
		} catch (IllegalArgumentException | IllegalStateException | ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		}
	}

	@PostMapping("/api/user/block/{userIdToBlock}")
	public ResponseEntity<ApiResponseWithData<UserSummaryDTO>> blockUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userIdToBlock) {
		User currentUser = userService.findUserByJwt(jwt);
		userService.blockUser(currentUser.getId(), userIdToBlock);
		User blockedUser = userService.findUserById(userIdToBlock);
		UserSummaryDTO dto = userSummaryMapper.mapToDTO(blockedUser);
		return ResponseEntity.ok(new ApiResponseWithData<>("User blocked successfully.", true, dto));
	}

	@PostMapping("/api/user/unblock/{userIdToUnblock}")
	public ResponseEntity<ApiResponseWithData<Void>> unblockUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID userIdToUnblock) {
		User currentUser = userService.findUserByJwt(jwt);
		userService.unblockUser(currentUser.getId(), userIdToUnblock);
		return ResponseEntity.ok(new ApiResponseWithData<>("User unblocked successfully.", true));
	}

	@GetMapping("/api/user/blocked")
	public ResponseEntity<ApiResponseWithData<List<UserSummaryDTO>>> getBlockedUsers(
			@RequestHeader("Authorization") String jwt) {
		try {
			User currentUser = userService.findUserByJwt(jwt);
			if (currentUser == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponseWithData<>("User not found.", false));
			}
			List<User> blockedUsers = userService.getBlockedUsers(currentUser.getId());
			List<UserSummaryDTO> blockedDTOs = userSummaryMapper.mapToDTOs(blockedUsers);
			return ResponseEntity.ok(new ApiResponseWithData<>("Blocked users retrieved successfully.", true,
					blockedDTOs));
		} catch (Exception ex) {
			logger.error("Error fetching blocked users", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error fetching blocked users.", false));
		}
	}

	@GetMapping("/api/user/preferences")
	public ResponseEntity<ApiResponseWithData<UserDTO>> getUserPreferences(@RequestHeader("Authorization") String jwt) {
		if (jwt == null || jwt.isBlank()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ApiResponseWithData<>("Authorization token is required.", false));
		}
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponseWithData<>("User not found.", false));
			}
			UserDTO userPreferences = userService.getUserPreferences(user.getId());
			return ResponseEntity.ok(new ApiResponseWithData<>("User preferences retrieved successfully.", true,
					userPreferences));
		} catch (Exception ex) {
			logger.error("Error fetching user preferences", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error fetching user preferences.", false));
		}
	}

	@GetMapping("/api/user/{userId}/followers")
	public ResponseEntity<ApiResponseWithData<List<UserSummaryDTO>>> getFollowers(@PathVariable UUID userId) {
		try {
			List<User> followers = userService.getUserFollowers(userId);
			List<UserSummaryDTO> followerDTOs = userSummaryMapper.mapToDTOs(followers);
			return ResponseEntity
					.ok(new ApiResponseWithData<>("Followers retrieved successfully.", true, followerDTOs));
		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error fetching followers.", false));
		}
	}

	@GetMapping("/api/user/{userId}/following")
	public ResponseEntity<ApiResponseWithData<List<UserSummaryDTO>>> getFollowing(
			@RequestHeader("Authorization") String jwt, @PathVariable UUID userId) {
		try {
			User currentUser = userService.findUserByJwt(jwt);
			if (!currentUser.getId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(new ApiResponseWithData<>("Access Denied.", false));
			}

			List<User> following = userService.getUserFollowing(userId);
			List<UserSummaryDTO> followingDTOs = userSummaryMapper.mapToDTOs(following);
			return ResponseEntity
					.ok(new ApiResponseWithData<>("Following retrieved successfully.", true, followingDTOs));
		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error fetching following.", false));
		}
	}

	@GetMapping("/users/count")
	public ResponseEntity<ApiResponseWithData<Long>> getUserCount() {
		try {
			Long count = userService.getUserCount();
			return ResponseEntity.ok(new ApiResponseWithData<>("User count retrieved successfully.", true, count));
		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error fetching user count.", false));
		}
	}

}
