package com.nix.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<Page<NotificationDTO>> getUserNotifications(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate,desc") String sort) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return ResponseEntity.status(401).build();
		}

		Sort sortOrder = parseSort(sort);
		Pageable pageable = PageRequest.of(page, size, sortOrder);
		Page<NotificationDTO> notifications = notificationService.getUserNotifications(user, pageable);
		return ResponseEntity.ok(notifications);
	}

	@GetMapping("/api/notifications/unread")
	public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdDate,desc") String sort) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return ResponseEntity.status(401).build();
		}

		Sort sortOrder = parseSort(sort);
		Pageable pageable = PageRequest.of(page, size, sortOrder);
		Page<NotificationDTO> unreadNotifications = notificationService.getUnreadNotifications(user, pageable);
		return ResponseEntity.ok(unreadNotifications);
	}

	@PutMapping("/api/notifications/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable UUID id, @RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return ResponseEntity.status(401).build();
		}

		notificationService.markAsRead(id, user);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/api/notifications/read-all")
	public ResponseEntity<Void> markAllAsRead(@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return ResponseEntity.status(401).build();
		}

		notificationService.markAllAsRead(user);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/admin/notifications/announce")
	public ResponseEntity<Void> createGlobalAnnouncement(@RequestHeader("Authorization") String jwt,
			@RequestBody String message) {
		User admin = userService.findUserByJwt(jwt);
		if (admin == null || !admin.getRole().getName().equals("ADMIN")) {
			return ResponseEntity.status(403).build();
		}

		notificationService.createGlobalAnnouncement(message);
		return ResponseEntity.ok().build();
	}

	private Sort parseSort(String sort) {
		String[] parts = sort.split(",");
		String property = parts[0];
		Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC
				: Sort.Direction.DESC;
		return Sort.by(direction, property);
	}
}
