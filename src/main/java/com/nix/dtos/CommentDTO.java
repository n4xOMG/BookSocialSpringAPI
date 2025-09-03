package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {

	private UUID id;
	private String content;
	private UserSummaryDTO user;
	private UUID bookId;
	private UUID chapterId;
	private UUID postId;
	private boolean isLikedByCurrentUser = false;
	private List<UserSummaryDTO> likedUsers;
	private LocalDateTime createdAt;
	private UUID parentCommentId;
	private List<CommentDTO> replyComment = new ArrayList<>();
}
