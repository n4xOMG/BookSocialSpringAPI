package com.nix.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.ReadingProgress;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.UserRepository;

@Service
public class ReadingProgressServiceImpl implements ReadingProgressService {

	@Autowired
	UserRepository userRepo;

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	ReadingProgressRepository readingProgressRepo;


	@Override
	public ReadingProgress findReadingProgressById(Integer progressId) throws IOException {
		Optional<ReadingProgress> progress = readingProgressRepo.findById(progressId);

		return progress.get();

	}

	@Override
	public ReadingProgress findByUserAndChapter(Integer userId, Integer chapterId) {
		return readingProgressRepo.findReadingProgressByChapterAndUserId(chapterId, userId);
	}

	@Override
	public List<ReadingProgress> findByUserAndBook(Integer userId, Integer bookId) {
		return readingProgressRepo.findReadingProgressByBookAndUserId(bookId, userId);
	}

	@Override
	public List<ReadingProgress> findAllReadingProgressByUserId(Integer userId) {
		return readingProgressRepo.findByUserId(userId);
	}

	@Override
	@Transactional
	public ReadingProgress createReadingProgress(ReadingProgress readingProgress) throws Exception {
		try {
			ReadingProgress newProgress = new ReadingProgress();

			newProgress.setUser(readingProgress.getUser());
			readingProgress.getUser().getReadingProgresses().add(newProgress);

			newProgress.setChapter(readingProgress.getChapter());
			newProgress.setProgress(readingProgress.getProgress());
			newProgress.setLastReadAt(LocalDateTime.now());

			return newProgress;
		} catch (Exception e) {
			throw new Exception("Error creating reading progress: " + e);
		}

	}

	@Override
	public ReadingProgress updateReadingProgress(Integer progressId, ReadingProgress readingProgress)
			throws Exception {
		try {
			ReadingProgress updateProgress = findReadingProgressById(progressId);

			if (readingProgress.getProgress() != null) {
				updateProgress.setProgress(readingProgress.getProgress());
				updateProgress.setLastReadAt(LocalDateTime.now());
			}

			return readingProgressRepo.save(updateProgress);
		} catch (Exception e) {
			throw new Exception("Error updating progress: " + e);
		}
	}

	@Override
	@Transactional
	public String deleteReadingProgress(Integer progressId) throws IOException {
		try {
			ReadingProgress deleteProgress = findReadingProgressById(progressId);

			deleteProgress.getUser().getReadingProgresses().remove(deleteProgress);

			deleteProgress.setChapter(null);
			deleteProgress.setUser(null);

			readingProgressRepo.delete(deleteProgress);

			return "Reading progress deleted successfully!";
		} catch (IOException e) {
			throw new IOException("Error deleting progress: " + e);
		}
	}

}
