package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.ChatDTO;
import com.nix.models.Chat;

public class ChatMapper implements Mapper<Chat, ChatDTO> {
	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();
	MessageMapper messageMapper = new MessageMapper();

	@Override
	public ChatDTO mapToDTO(Chat c) {
		if (c != null) {
			ChatDTO chatDTO = new ChatDTO();
			if (c.getId() != null) {
				chatDTO.setId(c.getId());
			}
			chatDTO.setMessages(messageMapper.mapToDTOs(c.getMessages()));
			chatDTO.setUserOne(userSummaryMapper.mapToDTO(c.getUserOne()));
			chatDTO.setUserTwo(userSummaryMapper.mapToDTO(c.getUserTwo()));
			return chatDTO;
		}
		return null;
	}

	@Override
	public List<ChatDTO> mapToDTOs(List<Chat> chats) {
		return chats.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
