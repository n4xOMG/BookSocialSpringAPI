package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nix.enums.NotificationEntityType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	private UUID id;

	private String message;

	private LocalDateTime createdDate;

	@JsonIgnore
	@ManyToOne
	private User receiver; // Receiver

	@OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserNotification> userNotifications = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	private NotificationEntityType entityType;
	private UUID entityId;
}
