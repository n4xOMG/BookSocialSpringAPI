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
public class ReadingProgressDTO {
	private Long id;
	private Long userId;
	private Long chapterId;
	private String chapterNum;
	private String chapterName;
	private Long bookId;
	private String bookTitle;
	private String bookCover;
	private String bookAuthor;
	private String bookArtist;
	private Double progress;
	private LocalDateTime lastReadAt;
}
