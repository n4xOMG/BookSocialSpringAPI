package com.nix.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Notification;
import com.nix.models.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	Page<Notification> findByUser(User user, Pageable pageable);

	@Query("SELECT n FROM Notification n " + "WHERE n.user = :user "
			+ "AND NOT EXISTS (SELECT un FROM UserNotification un WHERE un.user = :user AND un.notification = n AND un.isRead = true)")
	Page<Notification> findUnreadByUser(@Param("user") User user, Pageable pageable);

	List<Notification> findAllByUser(User user);
}
