package com.nix.dtos;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularChapterDTO {
	private UUID id;
	private String title;
	private String bookTitle;
	private String authorName;
	private Long unlockCount;
	private Integer price;
}