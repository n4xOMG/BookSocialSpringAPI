package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.NotificationDTO;
import com.nix.models.Notification;

public class NotificationMapper implements Mapper<Notification, NotificationDTO> {
	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Override
	public NotificationDTO mapToDTO(Notification n) {
		if (n != null) {
			NotificationDTO notificationDTO = new NotificationDTO();
			if (n.getId() != null) {
				notificationDTO.setId(n.getId());
			}
			notificationDTO.setMessage(n.getMessage());
			notificationDTO.setCreatedDate(n.getCreatedDate());
			notificationDTO.setRead(n.isRead());
			notificationDTO.setUserSummaryDTO(userSummaryMapper.mapToDTO(n.getUser()));
			return notificationDTO;
		}
		return null;
	}

	@Override
	public List<NotificationDTO> mapToDTOs(List<Notification> notifications) {
		return notifications.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
