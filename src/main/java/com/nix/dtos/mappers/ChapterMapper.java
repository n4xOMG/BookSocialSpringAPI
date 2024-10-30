package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.ChapterDTO;
import com.nix.models.Chapter;

public class ChapterMapper implements Mapper<Chapter, ChapterDTO> {

	@Override
	public List<ChapterDTO> mapToDTOs(List<Chapter> chapters) {
		return chapters.stream().map(this::mapToDTO).collect(Collectors.toList());

	}

	@Override
	public ChapterDTO mapToDTO(Chapter chapter) {
		ChapterDTO chapterDTO = new ChapterDTO();

		chapterDTO.setId(chapter.getId());
		chapterDTO.setChapterNum(chapter.getChapterNum());
		chapterDTO.setTitle(chapter.getTitle());
		chapterDTO.setBookId(chapter.getBook().getId());
		chapterDTO.setTranslatorId(chapter.getTranslatorId());
		chapterDTO.setContent(chapter.getContent());
		chapterDTO.setUploadDate(chapter.getUploadDate());

		return chapterDTO;
	}
}
