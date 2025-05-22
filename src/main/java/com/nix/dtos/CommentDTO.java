package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {

	private Long id;
	private String content;
	private UserSummaryDTO user;
	private Long bookId;
	private Long chapterId;
	private Long postId;
	private boolean isLikedByCurrentUser = false;
	private List<UserSummaryDTO> likedUsers;
	private LocalDateTime createdAt;
	private Long parentCommentId;
	private List<CommentDTO> replyComment = new ArrayList<>();
}
