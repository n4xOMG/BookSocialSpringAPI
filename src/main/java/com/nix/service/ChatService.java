package com.nix.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nix.dtos.MessageDTO;
import com.nix.models.Chat;
import com.nix.models.Message;
import com.nix.models.User;

public interface ChatService {
	Chat createOrGetChat(User userOne, User userTwo);

	Optional<Chat> findById(UUID id);

	List<Chat> getChatsForUser(User user);

	Message saveMessage(Chat chat, MessageDTO message);

	void deleteChat(UUID chatId, User currentUser) throws java.nio.file.AccessDeniedException;
}