package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.ReadingProgressDTO;
import com.nix.models.ReadingProgress;

@Component
public class ReadingProgressMapper implements Mapper<ReadingProgress, ReadingProgressDTO> {

	@Override
	public ReadingProgressDTO mapToDTO(ReadingProgress progress) {
		ReadingProgressDTO progressDTO = new ReadingProgressDTO();
		if (progress.getId() != null) {
			progressDTO.setId(progress.getId());
		}
		if (progress.getChapter() != null) {
			progressDTO.setChapterId(progress.getChapter().getId());
			progressDTO.setChapterNum(progress.getChapter().getChapterNum());
			progressDTO.setChapterName(progress.getChapter().getTitle());
			if (progress.getChapter().getBook() != null) {
				progressDTO.setBookId(progress.getChapter().getBook().getId());
				progressDTO.setBookTitle(progress.getChapter().getBook().getTitle());
				if (progress.getChapter().getBook().getBookCover() != null) {
					progressDTO.setBookCover(
							new com.nix.dtos.ImageAttachmentDTO(progress.getChapter().getBook().getBookCover().getUrl(),
									progress.getChapter().getBook().getBookCover().getIsMild()));
				}
				progressDTO.setBookAuthor(progress.getChapter().getBook().getAuthorName());
				progressDTO.setBookArtist(progress.getChapter().getBook().getArtistName());
			}
		}
		progressDTO.setLastReadAt(progress.getLastReadAt());
		progressDTO.setProgress(progress.getProgress());
		if (progress.getUser() != null) {
			progressDTO.setUserId(progress.getUser().getId());
		}
		return progressDTO;
	}

	@Override
	public List<ReadingProgressDTO> mapToDTOs(List<ReadingProgress> progresses) {
		return progresses.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
