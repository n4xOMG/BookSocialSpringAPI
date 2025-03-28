package com.nix.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.nix.models.Report;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ChapterUnlockRecordRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.ReportRepository;
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
	ReportRepository reportRepository;

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
	public List<Chapter> findNotDraftedChaptersByBookId(Integer bookId) {
		return chapterRepo.findNotDraftedChaptersByBookId(bookId);
	}

	@Override
	public List<Chapter> getAllChapters() {
		return chapterRepo.findAll();
	}

	@Override
	public Chapter createDraftChapter(Integer bookId, Chapter chapter) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		chapter.setBook(book);
		chapter.setUploadDate(LocalDateTime.now());
		chapter.setRoomId(UUID.randomUUID().toString());
		chapter.setDraft(true);

		return chapterRepo.save(chapter);
	}

	@Override
	@Transactional
	public Chapter publishChapter(Integer bookId, Chapter chapter) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		chapter.setBook(book);
		chapter.setComments(new ArrayList<>());
		chapter.setDraft(false);
		chapter.setUploadDate(LocalDateTime.now());
		List<User> followers = book.getFavoured();
		String message = "New chapter added to " + book.getTitle() + ": " + chapter.getTitle();
		for (User user : followers) {
			notificationService.createNotification(user, message);
		}
		return chapterRepo.save(chapter);
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
		if (chapter.getContent() != null) {
			editChapter.setContent(chapter.getContent());
		}
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
			// Step 1: Disassociate chapter from its book
			deleteChapter.setBook(null);

			// Step 2: Remove all progress related to this chapter
			progressRepo.deleteByChapterId(chapterId);

			// Step 3: Disassociate chapter from all users who liked it
			List<User> users = deleteChapter.getLikedUsers();
			for (User user : users) {
				user.getLikedChapters().remove(deleteChapter);
			}
			deleteChapter.getLikedUsers().clear(); // Clear the list to prevent memory leaks

			// Step 4: Handle Reports referencing this chapter, if applicable
			List<Report> reports = reportRepository.findByChapterId(chapterId);
			for (Report report : reports) {
				report.setChapter(null);
				reportRepository.save(report);
			}

			// Step 5: Proceed to delete the chapter
			chapterRepo.delete(deleteChapter);

			return "Chapter deleted successfully!";
		} catch (Exception e) {
			// Log the error for debugging purposes
			System.err.println("Error deleting chapter: " + e.getMessage());
			throw new Exception("Error deleting chapter: " + e.getMessage(), e);
		}
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
	public Chapter getChapterByRoomId(String roomId) {
		return chapterRepo.findByRoomId(roomId).get();
	}

	@Override
	@Transactional
	public Boolean likeChapter(Integer userId, Integer chapterId) throws Exception {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		Chapter chapter = chapterRepo.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + chapterId));

		if (!user.getLikedChapters().contains(chapter)) {
			user.getLikedChapters().add(chapter);
			chapter.getLikedUsers().add(user);
			userRepository.save(user);
			chapterRepo.save(chapter);

			return true;
		} else {
			user.getLikedChapters().remove(chapter);
			chapter.getLikedUsers().remove(user);
			userRepository.save(user);
			chapterRepo.save(chapter);

			return false;
		}
	}

	@Override
	@Transactional
	public Chapter unlikeChapter(Integer userId, Integer chapterId) throws Exception {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		Chapter chapter = chapterRepo.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + chapterId));

		if (user.getLikedChapters().contains(chapter)) {
			user.getLikedChapters().remove(chapter);
			chapter.getLikedUsers().remove(user);
			userRepository.save(user);
			return chapterRepo.save(chapter);
		}
		return chapter;
	}

	@Override
	public boolean isChapterLikedByUser(Integer userId, Integer chapterId) {
		Optional<User> userOpt = userRepository.findById(userId);
		Optional<Chapter> chapterOpt = chapterRepo.findById(chapterId);
		if (userOpt.isPresent() && chapterOpt.isPresent()) {
			return userOpt.get().getLikedChapters().contains(chapterOpt.get());
		}
		return false;
	}

}
