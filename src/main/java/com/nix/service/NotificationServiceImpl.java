package com.nix.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.nix.models.Notification;
import com.nix.models.User;
import com.nix.repository.NotificationRepository;
import com.nix.repository.UserRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	NotificationRepository notificationRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Override
	public void createNotification(User user, String message) {
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setMessage(message);
		notification.setRead(false);
		notification.setCreatedDate(System.currentTimeMillis());
		notificationRepository.save(notification);
		
		messagingTemplate.convertAndSendToUser(user.getUsername(), "/topic/notifications", notification);
	}

	@Override
	public List<Notification> getUserNotifications(User user) {
		return notificationRepository.findByUser(user);
	}

	@Override
	public List<Notification> getUnreadNotifications(User user) {
		return notificationRepository.findByUserAndIsReadFalse(user);
	}

	@Override
	public void markAsRead(Long notificationId) {
		notificationRepository.findById(notificationId).ifPresent(notification -> {
			notification.setRead(true);
			notificationRepository.save(notification);
		});

	}

	@Override
	public void createGlobalAnnouncement(String message) {
		List<User> users = userRepository.findAll();
		for (User user : users) {
			createNotification(user, message);
		}

	}

}
