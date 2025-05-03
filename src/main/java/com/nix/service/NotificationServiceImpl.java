package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.nix.dtos.NotificationDTO;
import com.nix.models.Notification;
import com.nix.models.User;
import com.nix.models.UserNotification;
import com.nix.repository.NotificationRepository;
import com.nix.repository.UserNotificationRepository;
import com.nix.repository.UserRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserNotificationRepository userNotificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate; // Add this for WebSocket messaging

	@Override
	public void createNotification(User user, String message) {
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setMessage(message);
		notification.setCreatedDate(LocalDateTime.now());
		notificationRepository.save(notification);

		// Send real-time update to the specific user
		messagingTemplate.convertAndSendToUser(user.getUsername(), // Destination based on username
				"/notifications", // User-specific topic/queue
				new NotificationDTO(notification.getId(), notification.getMessage(), false,
						notification.getCreatedDate()));
	}

	@Override
	public List<NotificationDTO> getUserNotifications(User user) {
		List<Notification> notifications = notificationRepository.findByUser(user);
		return notifications.stream().map(notification -> {
			boolean isRead = userNotificationRepository.existsByUserAndNotification(user, notification);
			return new NotificationDTO(notification.getId(), notification.getMessage(), isRead,
					notification.getCreatedDate());
		}).collect(Collectors.toList());
	}

	@Override
	public List<NotificationDTO> getUnreadNotifications(User user) {
	    List<Notification> notifications = notificationRepository.findByUser(user);
	    List<Long> readNotificationIds = userNotificationRepository.findReadNotificationIdsByUser(user);

	    return notifications.stream()
	        .filter(notification -> !readNotificationIds.contains(notification.getId()))
	        .map(notification -> new NotificationDTO(notification.getId(), notification.getMessage(), false, notification.getCreatedDate()))
	        .collect(Collectors.toList());
	}

	public void markAsRead(Long notificationId, User user) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new RuntimeException("Notification not found"));

		UserNotification userNotification = userNotificationRepository.findByUserAndNotification(user, notification)
				.orElse(new UserNotification());

		userNotification.setUser(user);
		userNotification.setNotification(notification);
		userNotification.setRead(true);
		userNotificationRepository.save(userNotification);
	}

	@Override
	public void createGlobalAnnouncement(String message) {
		List<User> users = userRepository.findAll();
		for (User user : users) {
			createNotification(user, message);
		}

		// Optionally broadcast to a group channel for all connected clients
		messagingTemplate.convertAndSend("/group/announcements", // Group destination
				message // Simple string message for announcements
		);
	}
}
