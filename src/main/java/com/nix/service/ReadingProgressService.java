package com.nix.service;

import java.io.IOException;
import java.util.List;

import com.nix.dtos.ReadingProgressDTO;
import com.nix.models.ReadingProgress;

public interface ReadingProgressService {
	ReadingProgressDTO findReadingProgressById(Integer progressId) throws IOException;

	ReadingProgressDTO findByUserAndChapter(Integer userId, Integer chapterId);

	List<ReadingProgressDTO> findByUserAndBook(Integer userId, Integer bookId);

	List<ReadingProgressDTO> findAllReadingProgressByUserId(Integer userId);

	ReadingProgressDTO createReadingProgress(ReadingProgress readingProgress) throws Exception;

	ReadingProgressDTO updateReadingProgress(Integer progressId, ReadingProgress readingProgress) throws Exception;

	String deleteReadingProgress(Integer progressId) throws IOException;
}
