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

		if (chapter != null) {
			if (chapter.getId() != null) {
				chapterDTO.setId(chapter.getId());
			}
			if (chapter.getRoomId() != null) {
				chapterDTO.setRoomId(chapter.getRoomId());
			}
			chapterDTO.setChapterNum(chapter.getChapterNum());
			chapterDTO.setTitle(chapter.getTitle());
			chapterDTO.setPrice(chapter.getPrice());
			chapterDTO.setLocked(chapter.isLocked());
			chapterDTO.setDraft(chapter.isDraft());
			chapterDTO.setAuthorId(chapter.getBook().getAuthor().getId());
			chapterDTO.setBookId(chapter.getBook().getId());
			chapterDTO.setContent(chapter.getContent());
			chapterDTO.setUploadDate(chapter.getUploadDate());
			return chapterDTO;
		}
		return null;
	}
}
