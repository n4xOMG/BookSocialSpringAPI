package com.nix.service;

import java.util.List;

import com.nix.models.Notification;
import com.nix.models.User;

public interface NotificationService {
	public void createNotification(User user, String message);

	public List<Notification> getUserNotifications(User user);

	public List<Notification> getUnreadNotifications(User user);

	public void markAsRead(Long notificationId);

	public void createGlobalAnnouncement(String message);
}
