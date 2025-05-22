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
public class ChapterSummaryDTO {
	private Long id;
	private String chapterNum;
	private String title;
	private int price;
	private boolean isLocked;
	private LocalDateTime uploadDate;
	private Long bookId;
	private boolean isUnlockedByUser = false;
}
