package com.nix.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChapterDTO {
	private Long id;
	private String roomId;
	private String chapterNum;
	private String title;
	private String content;
	private int price;
	private boolean isLocked;
	private boolean isDraft;
	private LocalDateTime uploadDate;
	private Long bookId;
	private List<CommentDTO> comments;
	private boolean isUnlockedByUser = false;
	private boolean isLikedByCurrentUser = false;
}
