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
public class ChatSummaryDTO {
	private Integer userId;
    private String fullname;
    private String avatarUrl;
    private String lastMessageContent;
    private LocalDateTime lastMessageTimestamp;
}
