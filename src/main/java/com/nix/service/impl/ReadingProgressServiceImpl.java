package com.nix.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.ReadingProgressDTO;
import com.nix.dtos.mappers.ReadingProgressMapper;
import com.nix.models.ReadingProgress;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.UserRepository;
import com.nix.service.ReadingProgressService;

@Service
public class ReadingProgressServiceImpl implements ReadingProgressService {

	@Autowired
	UserRepository userRepo;

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	ReadingProgressRepository readingProgressRepo;

	@Autowired
	ReadingProgressMapper progressMapper; // Add mapper injection

	@Override
	public ReadingProgressDTO findReadingProgressById(UUID progressId) throws IOException {
		Optional<ReadingProgress> progress = readingProgressRepo.findById(progressId);
		return progress.map(progressMapper::mapToDTO).orElseThrow(() -> new IOException("Reading progress not found"));
	}

	@Override
	public ReadingProgressDTO findByUserAndChapter(UUID userId, UUID chapterId) {
		ReadingProgress progress = readingProgressRepo.findReadingProgressByChapterAndUserId(chapterId, userId);
		return progress != null ? progressMapper.mapToDTO(progress) : null;
	}

	@Override
	public List<ReadingProgressDTO> findByUserAndBook(UUID userId, UUID bookId) {
		List<ReadingProgress> progresses = readingProgressRepo.findReadingProgressByBookAndUserId(bookId, userId);
		return progressMapper.mapToDTOs(progresses);
	}

	@Override
	public List<ReadingProgressDTO> findAllReadingProgressByUserId(UUID userId) {
		List<ReadingProgress> progresses = readingProgressRepo.findByUserId(userId);
		return progressMapper.mapToDTOs(progresses);
	}

	@Override
	@Transactional
	public ReadingProgressDTO createReadingProgress(ReadingProgress readingProgress) throws Exception {
		try {
			ReadingProgress newProgress = new ReadingProgress();
			newProgress.setUser(readingProgress.getUser());
			readingProgress.getUser().getReadingProgresses().add(newProgress);
			newProgress.setChapter(readingProgress.getChapter());
			newProgress.setProgress(readingProgress.getProgress());
			newProgress.setLastReadAt(LocalDateTime.now());

			return progressMapper.mapToDTO(readingProgressRepo.save(newProgress));
		} catch (Exception e) {
			throw new Exception("Error creating reading progress: " + e);
		}
	}

	@Override
	public ReadingProgressDTO updateReadingProgress(UUID progressId, ReadingProgress readingProgress) throws Exception {
		try {
			ReadingProgress updateProgress = readingProgressRepo.findById(progressId)
					.orElseThrow(() -> new Exception("Reading progress not found"));

			if (readingProgress.getProgress() != null) {
				updateProgress.setProgress(readingProgress.getProgress());
				updateProgress.setLastReadAt(LocalDateTime.now());
			}

			return progressMapper.mapToDTO(readingProgressRepo.save(updateProgress));
		} catch (Exception e) {
			throw new Exception("Error updating progress: " + e);
		}
	}

	@Override
	@Transactional
	public String deleteReadingProgress(UUID progressId) throws IOException {
		try {
			ReadingProgress deleteProgress = readingProgressRepo.findById(progressId)
					.orElseThrow(() -> new IOException("Reading progress not found"));

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
