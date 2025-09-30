package com.nix.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.nix.dtos.UserDTO;
import com.nix.dtos.mappers.UserMapper;
import com.nix.models.User;
import com.nix.service.AuthorService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/admin")
public class AdminController {
	@Autowired
	UserService userService;

	@Autowired
	AuthorService authorService;

	UserMapper userMapper = new UserMapper();

	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String searchTerm) {

		try {
			Page<User> usersPage = userService.getAllUsers(page, size, searchTerm);

			return new ResponseEntity<>(userMapper.mapToDTOs(usersPage.getContent()), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// ===== Payouts (Admin) =====
	@GetMapping("/payouts")
	public ResponseEntity<?> listPayouts(@RequestHeader("Authorization") String jwt,
			@RequestParam(required = false) com.nix.models.AuthorPayout.PayoutStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "requestedDate,desc") String sort) {
		try {
			User admin = userService.findUserByJwt(jwt);
			if (admin == null)
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");

			String[] parts = sort.split(",");
			String sortField = parts.length > 0 ? parts[0] : "requestedDate";
			String sortDir = parts.length > 1 ? parts[1] : "desc";
			Sort sortSpec = Sort.by(sortField);
			sortSpec = "asc".equalsIgnoreCase(sortDir) ? sortSpec.ascending() : sortSpec.descending();
			Pageable pageable = PageRequest.of(page, size, sortSpec);

			Page<AuthorPayoutDTO> payouts = authorService.listPayouts(status, pageable);
			return ResponseEntity.ok(payouts);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

	@PostMapping("/payouts/{payoutId}/process")
	public ResponseEntity<?> processPayout(@RequestHeader("Authorization") String jwt, @PathVariable UUID payoutId) {
		try {
			User admin = userService.findUserByJwt(jwt);
			if (admin == null)
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
			AuthorPayoutDTO updated = authorService.processPayout(payoutId);
			return ResponseEntity.ok(updated);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("/users/total")
	public ResponseEntity<Long> getTotalUsers(@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		return ResponseEntity.ok(userService.getTotalUsers());
	}

	// Get banned users count
	@GetMapping("/users/banned")
	public ResponseEntity<Long> getBannedUsersCount(@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		return ResponseEntity.ok(userService.getBannedUsersCount());
	}

	// Get suspended users count
	@GetMapping("/users/suspended")
	public ResponseEntity<Long> getSuspendedUsersCount(@RequestHeader("Authorization") String jwt) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		return ResponseEntity.ok(userService.getSuspendedUsersCount());
	}

	@PutMapping("/users/update/{userId}")
	public ResponseEntity<?> updateUser(@PathVariable UUID userId, @RequestBody User user) throws Exception {
		try {
			UserDTO updateUser = userMapper.mapToDTO(userService.updateUser(userId, user));

			return new ResponseEntity<>(updateUser, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/users/delete/{userId}")
	public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String jwt, @PathVariable UUID userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			return new ResponseEntity<>(userService.deleteUser(userId), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PatchMapping("/users/suspend/{userId}")
	public ResponseEntity<?> suspendUser(@RequestHeader("Authorization") String jwt, @PathVariable UUID userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			User suspendedUser = userService.suspendUser(userId);
			return new ResponseEntity<>(userMapper.mapToDTO(suspendedUser), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PatchMapping("/users/unsuspend/{userId}")
	public ResponseEntity<?> unsuspendUser(@RequestHeader("Authorization") String jwt, @PathVariable UUID userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			User unsuspendedUser = userService.unsuspendUser(userId);
			return new ResponseEntity<>(userMapper.mapToDTO(unsuspendedUser), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PatchMapping("/users/ban/{userId}")
	public ResponseEntity<?> banUser(@RequestHeader("Authorization") String jwt,
			@RequestBody Map<String, String> request) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			UUID userId = UUID.fromString(request.get("userId"));
			String banReason = request.get("banReason");

			User bannedUser = userService.banUser(userId, banReason);
			return new ResponseEntity<>(userMapper.mapToDTO(bannedUser), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PatchMapping("/users/unban/{userId}")
	public ResponseEntity<?> unbanUser(@RequestHeader("Authorization") String jwt, @PathVariable UUID userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			User unbannedUser = userService.unbanUser(userId);
			return new ResponseEntity<>(userMapper.mapToDTO(unbannedUser), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/users/{id}/role")
	public ResponseEntity<?> updateUserRole(@PathVariable("id") UUID userId, @RequestParam String roleName) {
		try {
			User updatedUser = userService.updateUserRole(userId, roleName);
			return ResponseEntity.ok(userMapper.mapToDTO(updatedUser));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
