package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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


	@Override
	public void createNotification(User user, String message) {
		Notification notification = new Notification();
		notification.setUser(user);
		notification.setMessage(message);
		notification.setRead(false);
		notification.setCreatedDate(LocalDateTime.now());
		notificationRepository.save(notification);
		
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
