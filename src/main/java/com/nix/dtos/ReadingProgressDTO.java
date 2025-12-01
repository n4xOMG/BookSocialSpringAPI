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
public class ReadingProgressDTO {
	private UUID id;
	private UUID userId;
	private UUID chapterId;
	private String chapterNum;
	private String chapterName;
	private UUID bookId;
	private String bookTitle;
	private ImageAttachmentDTO bookCover;
	private String bookAuthor;
	private String bookArtist;
	private Double progress;
	private LocalDateTime lastReadAt;
}
