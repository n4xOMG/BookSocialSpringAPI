package com.nix.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.NotificationDTO;
import com.nix.dtos.mappers.NotificationMapper;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.NotificationService;
import com.nix.service.UserService;

@RestController
public class NotificationController {
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserService userService;

	NotificationMapper notificationMapper = new NotificationMapper();

	@GetMapping("/api/notifications")
	public ResponseEntity<ApiResponseWithData<Page<NotificationDTO>>> getUserNotifications(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate,desc") String sort) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated.");
		}

		Sort sortOrder = parseSort(sort);
		Pageable pageable = PageRequest.of(page, size, sortOrder);
		Page<NotificationDTO> notifications = notificationService.getUserNotifications(user, pageable);
		return buildSuccessResponse("Notifications retrieved successfully.", notifications);
	}

	@GetMapping("/api/notifications/unread")
	public ResponseEntity<ApiResponseWithData<Page<NotificationDTO>>> getUnreadNotifications(
			@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate,desc") String sort) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated.");
		}

		Sort sortOrder = parseSort(sort);
		Pageable pageable = PageRequest.of(page, size, sortOrder);
		Page<NotificationDTO> unreadNotifications = notificationService.getUnreadNotifications(user, pageable);
		return buildSuccessResponse("Unread notifications retrieved successfully.", unreadNotifications);
	}

	@PutMapping("/api/notifications/{id}/read")
	public ResponseEntity<ApiResponseWithData<Void>> markAsRead(@PathVariable UUID id,
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated.");
		}

		notificationService.markAsRead(id, user);
		return buildSuccessResponse("Notification marked as read.", null);
	}

	@PutMapping("/api/notifications/read-all")
	public ResponseEntity<ApiResponseWithData<Void>> markAllAsRead(@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not authenticated.");
		}

		notificationService.markAllAsRead(user);
		return buildSuccessResponse("All notifications marked as read.", null);
	}

	@PostMapping("/admin/notifications/announce")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<Void>> createGlobalAnnouncement(
			@RequestHeader("Authorization") String jwt,
			@RequestBody String message) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null || !admin.getRole().getName().equals("ADMIN")) {
			return buildErrorResponse(HttpStatus.FORBIDDEN, "Only admins can create announcements.");
		}

		notificationService.createGlobalAnnouncement(message);
		return buildSuccessResponse("Announcement created successfully.", null);
	}

	private Sort parseSort(String sort) {
		String[] parts = sort.split(",");
		String property = parts[0];
		Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
				: Sort.Direction.DESC;
		return Sort.by(direction, property);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}
