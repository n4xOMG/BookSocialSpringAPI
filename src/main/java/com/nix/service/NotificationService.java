package com.nix.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.NotificationDTO;
import com.nix.enums.NotificationEntityType;
import com.nix.models.User;

public interface NotificationService {
	public void createNotification(User user, String message, NotificationEntityType entityType, UUID entityId);

	public Page<NotificationDTO> getUserNotifications(User user, Pageable pageable);

	public Page<NotificationDTO> getUnreadNotifications(User user, Pageable pageable);

	public void markAllAsRead(User user);

	public void markAsRead(UUID notificationId, User user);

	public void createGlobalAnnouncement(String message);
}
