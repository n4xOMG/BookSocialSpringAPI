package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nix.models.Notification;
import com.nix.models.User;
import com.nix.models.UserNotification;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

	boolean existsByUserAndNotification(User user, Notification notification);

	boolean existsByUserAndNotificationAndIsRead(User user, Notification notification, boolean isRead);

	Optional<UserNotification> findByUserAndNotification(User user, Notification notification);

	// Lấy danh sách các notificationId mà user đã đọc
	@Query("SELECT un.notification.id FROM UserNotification un WHERE un.user = :user")
	List<UUID> findReadNotificationIdsByUser(@Param("user") User user);
}
