package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Notification;
import com.nix.models.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findByUser(User user);
}

