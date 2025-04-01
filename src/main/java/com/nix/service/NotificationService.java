package com.nix.service;

import java.util.List;

import com.nix.dtos.NotificationDTO;
import com.nix.models.User;

public interface NotificationService {
	public void createNotification(User user, String message);

	public List<NotificationDTO> getUserNotifications(User user);

	public List<NotificationDTO> getUnreadNotifications(User user);

	public void markAsRead(Long notificationId, User user);

	public void createGlobalAnnouncement(String message);
}
