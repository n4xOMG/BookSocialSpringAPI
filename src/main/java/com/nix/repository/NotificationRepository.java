package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Notification;
import com.nix.models.User;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	Page<Notification> findByReceiver(User user, Pageable pageable);

	@Query("SELECT n FROM Notification n " + "WHERE n.receiver = :user "
			+ "AND NOT EXISTS (SELECT un FROM UserNotification un WHERE un.user = :user AND un.notification = n AND un.isRead = true)")
	Page<Notification> findUnreadByReceiver(@Param("user") User user, Pageable pageable);

	List<Notification> findAllByReceiver(User user);
}
