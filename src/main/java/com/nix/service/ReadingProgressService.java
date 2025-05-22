package com.nix.service;

import java.io.IOException;
import java.util.List;

import com.nix.dtos.ReadingProgressDTO;
import com.nix.models.ReadingProgress;

public interface ReadingProgressService {
	ReadingProgressDTO findReadingProgressById(Long progressId) throws IOException;

	ReadingProgressDTO findByUserAndChapter(Long userId, Long chapterId);

	List<ReadingProgressDTO> findByUserAndBook(Long userId, Long bookId);

	List<ReadingProgressDTO> findAllReadingProgressByUserId(Long userId);

	ReadingProgressDTO createReadingProgress(ReadingProgress readingProgress) throws Exception;

	ReadingProgressDTO updateReadingProgress(Long progressId, ReadingProgress readingProgress) throws Exception;

	String deleteReadingProgress(Long progressId) throws IOException;
}
