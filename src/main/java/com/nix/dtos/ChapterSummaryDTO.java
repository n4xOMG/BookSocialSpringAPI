package com.nix.dtos;

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
public class ChapterSummaryDTO {
	private UUID id;
	private String chapterNum;
	private String title;
	private int price;
	private UUID authorId;
	private boolean isLocked;
	private LocalDateTime uploadDate;
	private UUID bookId;
	private boolean isUnlockedByUser = false;
}
