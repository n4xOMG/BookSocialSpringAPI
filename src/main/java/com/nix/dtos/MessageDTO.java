package com.nix.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long chatId;
	private UserSummaryDTO sender;
	private UserSummaryDTO receiver;
	private String imageUrl;
	private String content;
	private LocalDateTime timestamp;
	private boolean isRead;
	
	@Override
	public String toString() {
	    return "MessageDTO{id=" + id + ", content='" + content + "', senderId=" + sender.getId() + ", receiverId=" + receiver.getId() + "}";
	}

}
