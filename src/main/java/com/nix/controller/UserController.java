package com.nix.controller;

import java.util.List;

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
import com.nix.service.UserService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class UserController {
	@Autowired
	UserService userService;

	UserMapper userMapper = new UserMapper();

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	UserSummaryMapper userSummaryDTO = new UserSummaryMapper();

	@GetMapping("/user/profile/{userId}")
	public ResponseEntity<?> getUserProfile(@PathVariable("userId") Integer userId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		try {
			User otherUser = userService.findUserById(userId);
			UserSummaryDTO userSummaryDTO = userSummaryMapper.mapToDTO(otherUser);

			if (jwt != null && !jwt.isEmpty()) {
				User currentUser = userService.findUserByJwt(jwt);

				boolean isFollowing = userService.isFollowedByCurrentUser(currentUser, otherUser);
				userSummaryDTO.setFollowedByCurrentUser(isFollowing);
			}

			return new ResponseEntity<>(userSummaryDTO, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/user/profile")
	public ResponseEntity<?> getUserFromToken(@RequestHeader("Authorization") String jwt) {

		try {
			User user = userService.findUserByJwt(jwt);

			return new ResponseEntity<>(userMapper.mapToDTO(user), HttpStatus.OK);
		} catch (JwtException e) {
			return new ResponseEntity<>("Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private String getSiteURL(HttpServletRequest request) {
		String siteURL = request.getRequestURL().toString();
		return siteURL.replace(request.getServletPath(), "");
	}

	@PutMapping("/api/user/profile")
	public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String jwt, @RequestBody User user,
			HttpServletRequest httpRequest) throws Exception {

		try {
			UserDTO updateUser = userMapper
					.mapToDTO(userService.updateCurrentSessionUser(jwt, user, getSiteURL(httpRequest)));

			return new ResponseEntity<>(updateUser, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/user/search")
	public ResponseEntity<?> searchUser(@RequestParam("query") String query) throws Exception {

		try {
			List<UserDTO> foundUsers = userMapper.mapToDTOs(userService.findUserByUsername(query));

			return new ResponseEntity<>(foundUsers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/follow/{userIdToFollow}")
	public ResponseEntity<?> followUser(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer userIdToFollow) {
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
			return ResponseEntity.ok(userSummaryDTO);
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@PostMapping("/unfollow/{userIdToUnfollow}")
	public ResponseEntity<?> unFollowUser(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer userIdToUnfollow) {
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
			return ResponseEntity.ok(userSummaryDTO);
		} catch (IllegalArgumentException | IllegalStateException | ResourceNotFoundException ex) {
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@GetMapping("/user/preferences")
	public ResponseEntity<UserDTO> getUserPreferences(@RequestHeader("Authorization") String jwt) {
		if (jwt != null) {
			User user = userService.findUserByJwt(jwt);
			UserDTO userPreferences = userService.getUserPreferences(user.getId());
			return ResponseEntity.ok(userPreferences);
		}
		return null;

	}

	@GetMapping("/api/user/{userId}/followers")
	public ResponseEntity<?> getFollowers(@PathVariable Integer userId) {
		try {

			List<User> followers = userService.getUserFollowers(userId);
			return ResponseEntity.ok(userSummaryMapper.mapToDTOs(followers));
		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching followers.");
		}
	}

	@GetMapping("/api/user/{userId}/following")
	public ResponseEntity<?> getFollowing(@RequestHeader("Authorization") String jwt, @PathVariable Integer userId) {
		try {
			User currentUser = userService.findUserByJwt(jwt);
			if (!currentUser.getId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied.");
			}

			List<User> following = userService.getUserFollowing(userId);
			return ResponseEntity.ok(userSummaryMapper.mapToDTOs(following));
		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching following.");
		}
	}

}
