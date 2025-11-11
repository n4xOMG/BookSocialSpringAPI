package com.nix.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.nix.dtos.NotificationDTO;
import com.nix.enums.NotificationEntityType;
import com.nix.models.Notification;
import com.nix.models.User;
import com.nix.models.UserNotification;
import com.nix.repository.NotificationRepository;
import com.nix.repository.UserNotificationRepository;
import com.nix.repository.UserRepository;
import com.nix.service.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private UserNotificationRepository userNotificationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Override
	public void createNotification(User user, String message, NotificationEntityType entityType, UUID entityId) {
		Notification notification = new Notification();
		notification.setReceiver(user);
		notification.setMessage(message);
		notification.setCreatedDate(LocalDateTime.now());
		notification.setEntityType(entityType);
		notification.setEntityId(entityId);
		notificationRepository.save(notification);

		messagingTemplate.convertAndSendToUser(user.getUsername(), "/notifications", new NotificationDTO(
				notification.getId(), notification.getMessage(), false, notification.getEntityType(),
				notification.getEntityId(),
				notification.getCreatedDate()));
	}

	@Override
	public Page<NotificationDTO> getUserNotifications(User user, Pageable pageable) {
		Page<Notification> notifications = notificationRepository.findByReceiver(user, pageable);
		return notifications.map(notification -> {
			boolean isRead = userNotificationRepository.existsByUserAndNotificationAndIsRead(user, notification, true);
			return new NotificationDTO(notification.getId(), notification.getMessage(), isRead,
					notification.getEntityType(),
					notification.getEntityId(),
					notification.getCreatedDate());
		});
	}

	@Override
	public Page<NotificationDTO> getUnreadNotifications(User user, Pageable pageable) {
		Page<Notification> notifications = notificationRepository.findUnreadByReceiver(user, pageable);
		return notifications
				.map(notification -> new NotificationDTO(notification.getId(), notification.getMessage(), false,
						notification.getEntityType(),
						notification.getEntityId(),
						notification.getCreatedDate()));
	}

	@Override
	public void markAsRead(UUID notificationId, User user) {
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
	public void markAllAsRead(User user) {
		List<Notification> notifications = notificationRepository.findAllByReceiver(user);
		List<UUID> readNotificationIds = userNotificationRepository.findReadNotificationIdsByUser(user);

		notifications.stream().filter(notification -> !readNotificationIds.contains(notification.getId()))
				.forEach(notification -> {
					UserNotification userNotification = new UserNotification();
					userNotification.setUser(user);
					userNotification.setNotification(notification);
					userNotification.setRead(true);
					userNotificationRepository.save(userNotification);
				});
	}

	@Override
	public void createGlobalAnnouncement(String message) {
		List<User> users = userRepository.findAll();
		for (User user : users) {
			createNotification(user, message, NotificationEntityType.GLOBAL, null);
		}

		messagingTemplate.convertAndSend("/group/announcements", message);
	}
}