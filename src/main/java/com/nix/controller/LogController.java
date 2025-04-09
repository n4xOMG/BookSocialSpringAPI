package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.service.LogReaderService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/admin")
public class LogController {

	@Autowired
	private LogReaderService logReaderService;

	@Autowired
	private UserService userService;

	@GetMapping("/logs")
	public ResponseEntity<?> getLogs(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(logReaderService.getRecentLogs(page, size));
	}

	@GetMapping("/logs/user/{username}")
	public ResponseEntity<?> getLogsByUser(@PathVariable String username, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(logReaderService.getLogsByUsername(username, page, size));
	}

	@GetMapping("/logs/current-user")
	public ResponseEntity<?> getCurrentUserLogs(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (jwt != null) {
			String username = userService.findUserByJwt(jwt).getEmail();
			return ResponseEntity.ok(logReaderService.getLogsByUsername(username, page, size));
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
	}
}