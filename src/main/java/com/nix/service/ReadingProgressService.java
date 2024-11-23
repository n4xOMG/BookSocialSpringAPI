package com.nix.service;

import java.io.IOException;
import java.util.List;

import com.nix.models.ReadingProgress;

public interface ReadingProgressService {
	public ReadingProgress findReadingProgressById(Integer progressId) throws IOException;

	public ReadingProgress findByUserAndChapter(Integer userId, Integer chapterId);

	public List<ReadingProgress> findByUserAndBook(Integer userId, Integer bookId);

	public List<ReadingProgress> findAllReadingProgressByUserId(Integer userId);

	public ReadingProgress createReadingProgress(ReadingProgress readingProgress) throws Exception;

	public ReadingProgress updateReadingProgress(Integer progressId, ReadingProgress readingProgress) throws Exception;

	public String deleteReadingProgress(Integer progressId) throws IOException;

}
