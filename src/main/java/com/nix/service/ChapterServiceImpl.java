package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ChapterUnlockRecordRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.UserRepository;

@Service
public class ChapterServiceImpl implements ChapterService {

	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	BookRepository bookRepo;

	@Autowired
	NotificationService notificationService;

	@Autowired
	ReadingProgressRepository progressRepo;

	@Autowired
	private ChapterUnlockRecordRepository unlockRecordRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public Chapter findChapterById(Integer chapterId) {
		Chapter chapter = chapterRepo.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Cannot found chapter with id: " + chapterId));
		return chapter;
	}

	@Override
	public List<Chapter> findChaptersByBookId(Integer bookId) {
		return chapterRepo.findByBookId(bookId);
	}

	@Override
	public List<Chapter> getAllChapters() {
		return chapterRepo.findAll();
	}

	@Override
	@Transactional
	public Chapter addChapterAndNotifyFollowers(Integer bookId, Chapter chapter) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		chapter.setBook(book);
		chapter.setUploadDate(LocalDateTime.now());
		chapter.setDeleted(false);
		chapter.setUploadDate(LocalDateTime.now());

		Chapter newChapter = chapterRepo.save(chapter);
		List<User> followers = book.getFavoured();
		String message = "New chapter added to " + book.getTitle() + ": " + chapter.getTitle();
		for (User user : followers) {
			notificationService.createNotification(user, message);
		}
		return newChapter;
	}

	@Override
	public Chapter editChapter(Integer chapterId, Chapter chapter) throws Exception {
		Chapter editChapter = findChapterById(chapterId);
		if (editChapter == null) {
			throw new Exception("Chapter not found");
		}
		editChapter.setChapterNum(chapter.getChapterNum());
		editChapter.setPrice(chapter.getPrice());
		editChapter.setTitle(chapter.getTitle());
		editChapter.setContent(chapter.getContent());
		editChapter.setLocked(chapter.isLocked());
		editChapter.setDraft(chapter.isDraft());
		return chapterRepo.save(editChapter);
	}

	@Override
	@Transactional
	public String deleteChapter(Integer chapterId) throws Exception {
		Chapter deleteChapter = findChapterById(chapterId);
		if (deleteChapter == null) {
			throw new Exception("Chapter not found");
		}

		try {
			deleteChapter.setBook(null);
			progressRepo.deleteByChapterId(chapterId);
			chapterRepo.delete(deleteChapter);

			return "Chapter deleted successfully!";
		} catch (Exception e) {
			return "Error deleting chapter: " + e.getMessage();
		}
	}

	@Override
	public void incrementChapterViewCount(Integer chapterId) {
		chapterRepo.incrementViewCount(chapterId);

	}

	@Override
	public void unlockChapter(Integer userId, Integer chapterId) throws Exception {
		Chapter chapter = chapterRepo.findById(chapterId).orElseThrow(() -> new Exception("Chapter not found"));

		if (!chapter.isLocked()) {
			throw new Exception("Chapter is not locked");
		}

		// Check if already unlocked
		Optional<ChapterUnlockRecord> existingUnlock = unlockRecordRepository.findByUserIdAndChapterId(userId,
				chapterId);
		if (existingUnlock.isPresent()) {
			throw new Exception("Chapter already unlocked");
		}

		int unlockCost = chapter.getPrice();
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.getCredits() >= unlockCost) {
			// Deduct credits
			user.setCredits(user.getCredits() - unlockCost);
			userRepository.save(user);

			// Create unlock record
			ChapterUnlockRecord unlockRecord = new ChapterUnlockRecord();
			unlockRecord.setUser(user);
			unlockRecord.setChapter(chapter);
			unlockRecord.setUnlockDate(LocalDateTime.now());
			unlockRecord.setUnlockCost(unlockCost);

			unlockRecordRepository.save(unlockRecord);
		} else {
			throw new Exception("Insufficient credits");
		}

	}

	@Override
	public boolean isChapterUnlockedByUser(Integer userId, Integer chapterId) {
		Optional<ChapterUnlockRecord> unlockRecord = unlockRecordRepository.findByUserIdAndChapterId(userId, chapterId);
		return unlockRecord.isPresent();
	}

	@Override
	public List<Chapter> findChaptersByBookIdWithUnlockStatus(Integer bookId, Integer userId) {
		List<Chapter> chapters = chapterRepo.findByBookId(bookId);

		chapters.forEach(chapter -> {
			if (chapter.getPrice() <= 0) {
				// Free chapters are always unlocked
				chapter.setUnlockedByUser(true);
			} else if (userId != null) {
				List<Integer> unlockedChapterIds = unlockRecordRepository.findChapterIdsByUserId(userId);
				if (unlockedChapterIds.contains(chapter.getId())) {
					chapter.setUnlockedByUser(true);
				}

			} else {
				chapter.setUnlockedByUser(false);
			}
		});

		return chapters;
	}

	@Override
	public Chapter createDraftChapter(Integer bookId, Chapter chapter) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		chapter.setBook(book);
		chapter.setUploadDate(LocalDateTime.now());
		chapter.setDeleted(false);
		chapter.setUploadDate(LocalDateTime.now());
		chapter.setRoomId(UUID.randomUUID().toString());
		chapter.setDraft(true);

		return chapterRepo.save(chapter);
	}

	@Override
	public Chapter getChapterByRoomId(String roomId) {
		return chapterRepo.findByRoomId(roomId).get();
	}

}
