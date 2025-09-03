package com.nix.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.nix.dtos.ReadingProgressDTO;
import com.nix.models.ReadingProgress;

public interface ReadingProgressService {
	ReadingProgressDTO findReadingProgressById(UUID progressId) throws IOException;

	ReadingProgressDTO findByUserAndChapter(UUID userId, UUID chapterId);

	List<ReadingProgressDTO> findByUserAndBook(UUID userId, UUID bookId);

	List<ReadingProgressDTO> findAllReadingProgressByUserId(UUID userId);

	ReadingProgressDTO createReadingProgress(ReadingProgress readingProgress) throws Exception;

	ReadingProgressDTO updateReadingProgress(UUID progressId, ReadingProgress readingProgress) throws Exception;

	String deleteReadingProgress(UUID progressId) throws IOException;
}
