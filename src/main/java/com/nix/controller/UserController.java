package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.UserDTO;
import com.nix.dtos.mappers.UserMapper;
import com.nix.models.User;
import com.nix.service.UserService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class UserController {
	@Autowired
	UserService userService;

	UserMapper userMapper = new UserMapper();

	@GetMapping("/api/user/profile/{userId}")
	public ResponseEntity<?> getUserProfile(@PathVariable("userId") Integer userId) throws Exception {
		try {
			User user = userService.findUserById(userId);

			return new ResponseEntity<>(userMapper.mapToDTO(user), HttpStatus.OK);
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
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	private String getSiteURL(HttpServletRequest request) {
		String siteURL = request.getRequestURL().toString();
		return siteURL.replace(request.getServletPath(), "");
	}
	@PutMapping("/api/user/profile")
	public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String jwt, @RequestBody User user, HttpServletRequest httpRequest)
			throws Exception {
		
		try {
			UserDTO updateUser = userMapper.mapToDTO(userService.updateCurrentSessionUser(jwt, user, getSiteURL(httpRequest)));

			return new ResponseEntity<>(updateUser, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
