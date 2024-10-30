package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.UserDTO;
import com.nix.dtos.mappers.UserMapper;
import com.nix.models.User;
import com.nix.service.UserService;

@RestController
@RequestMapping("/admin")
public class AdminController {
	@Autowired
	UserService userService;

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

	@PutMapping("/users/update/{userId}")
	public ResponseEntity<?> updateUser(@PathVariable Integer userId, @RequestBody User user) throws Exception {
		try {
			UserDTO updateUser = userMapper.mapToDTO(userService.updateUser(userId, user));

			return new ResponseEntity<>(updateUser, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/users/delete/{userId}")
	public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String jwt, @PathVariable Integer userId)
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
	public ResponseEntity<?> suspendUser(@RequestHeader("Authorization") String jwt, @PathVariable Integer userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			return new ResponseEntity<>(userService.suspendUser(userId), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PatchMapping("/users/unsuspend/{userId}")
	public ResponseEntity<?> unsuspendUser(@RequestHeader("Authorization") String jwt, @PathVariable Integer userId)
			throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}
			return new ResponseEntity<>(userService.unsuspendUser(userId), HttpStatus.OK);

		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
