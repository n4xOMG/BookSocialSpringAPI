package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.ChapterSummaryDTO;
import com.nix.models.Chapter;

public class ChapterSummaryMapper implements Mapper<Chapter, ChapterSummaryDTO> {

	@Override
	public ChapterSummaryDTO mapToDTO(Chapter c) {
		ChapterSummaryDTO chapterSummaryDTO = new ChapterSummaryDTO();
		if (c != null) {
			chapterSummaryDTO.setBookId(c.getBook().getId());
			if (c.getId() != null) {
				chapterSummaryDTO.setId(c.getId());
			}
			chapterSummaryDTO.setChapterNum(c.getChapterNum());
			chapterSummaryDTO.setLocked(c.isLocked());
			chapterSummaryDTO.setPrice(c.getPrice());
			chapterSummaryDTO.setAuthorId(c.getBook().getAuthor().getId());
			chapterSummaryDTO.setTitle(c.getTitle());
			chapterSummaryDTO.setUploadDate(c.getUploadDate());
			return chapterSummaryDTO;
		}
		return null;
	}

	@Override
	public List<ChapterSummaryDTO> mapToDTOs(List<Chapter> chapters) {
		return chapters.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
