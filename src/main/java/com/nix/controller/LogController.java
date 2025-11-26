package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.response.ApiResponseWithData;
import com.nix.service.LogReaderService;
import com.nix.service.UserService;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN')")
public class LogController {

	@Autowired
	private LogReaderService logReaderService;

	@Autowired
	private UserService userService;

	@GetMapping("/logs")
	public ResponseEntity<ApiResponseWithData<Object>> getLogs(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		try {
			Object logs = logReaderService.getRecentLogs(page, size);
			return buildSuccessResponse("Logs retrieved successfully.", logs);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve logs: " + e.getMessage());
		}
	}

	@GetMapping("/logs/user/{username}")
	public ResponseEntity<ApiResponseWithData<Object>> getLogsByUser(@PathVariable String username,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		try {
			Object logs = logReaderService.getLogsByUsername(username, page, size);
			return buildSuccessResponse("Logs retrieved successfully.", logs);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve logs for user: " + e.getMessage());
		}
	}

	@GetMapping("/logs/current-user")
	public ResponseEntity<ApiResponseWithData<Object>> getCurrentUserLogs(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (jwt == null || jwt.isBlank()) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		try {
			String username = userService.findUserByJwt(jwt).getEmail();
			Object logs = logReaderService.getLogsByUsername(username, page, size);
			return buildSuccessResponse("Logs retrieved successfully.", logs);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to retrieve current user logs: " + e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}