package com.nix.dtos;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
	private Long id;
	private Long chatId;
	private UserSummaryDTO sender;
	private UserSummaryDTO receiver;
	private String imageUrl;
	private String content;
	private LocalDateTime timestamp;
	private boolean isRead;

}
