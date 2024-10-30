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

	private Integer id;
	private String content;
	private UserSummaryDTO user;
	private Integer bookId;
	private Integer chapterId;
	private Integer likedUsers;
	private LocalDateTime createdAt;
	private List<CommentDTO> replyComment = new ArrayList<>();
}
