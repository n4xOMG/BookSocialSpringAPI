package com.nix.dtos;

import java.time.LocalDateTime;
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
public class ChapterDTO {
	private UUID id;
	private String roomId;
	private String chapterNum;
	private String title;
	private String content;
	private int price;
	private UUID authorId;
	private boolean isLocked;
	private boolean isDraft;
	private LocalDateTime uploadDate;
	private UUID bookId;
	private List<CommentDTO> comments;
	private boolean isUnlockedByUser = false;
	private boolean isLikedByCurrentUser = false;
}
