package com.nix.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
	private Long id;
    private Integer senderId;
    private Integer receiverId;
    private String content;
    private String imageUrl;
    private LocalDateTime timestamp;
    private boolean isRead;
}
