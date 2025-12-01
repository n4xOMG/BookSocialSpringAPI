package com.nix.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

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
	private UUID id;
	private UUID chatId;
	private UserSummaryDTO sender;
	private UserSummaryDTO receiver;
	private ImageAttachmentDTO image;
	private String content;
	private LocalDateTime timestamp;
	private boolean isRead;

	@Override
	public String toString() {
		return "MessageDTO{id=" + id + ", content='" + content + "', senderId=" + sender.getId() + ", receiverId="
				+ receiver.getId() + "}";
	}

}
