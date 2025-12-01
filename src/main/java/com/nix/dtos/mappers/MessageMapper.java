package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.MessageDTO;
import com.nix.models.Message;

public class MessageMapper implements Mapper<Message, MessageDTO> {

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Override
	public MessageDTO mapToDTO(Message m) {
		MessageDTO messageDTO = new MessageDTO();
		if (m != null) {
			if (m.getId() != null) {
				messageDTO.setId(m.getId());
			}
			messageDTO.setChatId(m.getChat().getId());
			messageDTO.setSender(userSummaryMapper.mapToDTO(m.getSender()));
			messageDTO.setReceiver(userSummaryMapper.mapToDTO(m.getSender()));
			messageDTO.setContent(m.getContent());
			if (m.getImage() != null) {
				messageDTO
						.setImage(new com.nix.dtos.ImageAttachmentDTO(m.getImage().getUrl(), m.getImage().getIsMild()));
			}
			messageDTO.setTimestamp(m.getTimestamp());
			messageDTO.setRead(m.isRead());
		}
		return messageDTO;
	}

	@Override
	public List<MessageDTO> mapToDTOs(List<Message> messages) {
		return messages.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
