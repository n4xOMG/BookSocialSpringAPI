package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.nix.enums.NotificationEntityType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
	private UUID id;
	private String message;
	private boolean isRead; // return read
	@Enumerated(EnumType.STRING)
	private NotificationEntityType notificationEntityType;
	private UUID entityId;
	private LocalDateTime createdDate;
}
