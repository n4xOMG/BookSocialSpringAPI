package com.nix.service;

import java.util.List;

import com.nix.dtos.ChatSummaryDTO;
import com.nix.dtos.MessageDTO;
import com.nix.models.Message;

public interface MessageService {
	// Save a new message to the database.
	public Message saveMessage(Integer senderUserid, Integer receiverUserid, MessageDTO message);

	// Retrieve message history between two users.
	public List<Message> getMessageHistory(Integer senderUserid, Integer receiverUserid);

	public List<Message> getUnreadMessages(Integer receiverUserid);
	
	List<ChatSummaryDTO> getUserChats(Integer userId) ;

	public void markMessageAsRead(Long messageId);

}
