package com.nix.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.enums.NotificationEntityType;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.AuthorEarning;
import com.nix.models.Book;
import com.nix.models.BookFavourite;
import com.nix.models.Chapter;
import com.nix.models.ChapterUnlockRecord;
import com.nix.models.Report;
import com.nix.models.User;
import com.nix.repository.AuthorEarningRepository;
import com.nix.repository.BookFavouriteRepository;
import com.nix.repository.BookRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.ChapterUnlockRecordRepository;
import com.nix.repository.ReadingProgressRepository;
import com.nix.repository.ReportRepository;
import com.nix.repository.UserRepository;
import com.nix.service.AuthorService;
import com.nix.service.ChapterService;
import com.nix.service.NotificationService;
import com.nix.service.UserWalletService;

import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;

@Service
public class ChapterServiceImpl implements ChapterService {

	private static final Logger logger = LoggerFactory.getLogger(ChapterService.class);
	@Autowired
	ChapterRepository chapterRepo;

	@Autowired
	BookRepository bookRepo;

	@Autowired
	BookFavouriteRepository bookFavouriteRepository;

	@Autowired
	NotificationService notificationService;

	@Autowired
	ReadingProgressRepository progressRepo;

	@Autowired
	ReportRepository reportRepository;

	@Autowired
	private ChapterUnlockRecordRepository unlockRecordRepository;

	@Autowired
	private AuthorEarningRepository authorEarningRepository;

	@Autowired
	private AuthorService authorService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserWalletService userWalletService;

	@Override
	public Chapter findChapterById(UUID chapterId) {
		Chapter chapter = chapterRepo.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Cannot found chapter with id: " + chapterId));
		return chapter;
	}

	@Override
	public List<Chapter> findChaptersByBookId(UUID bookId) {
		return chapterRepo.findByBookIdOrderByUploadDateAsc(bookId);
	}

	@Override
	public List<Chapter> findChaptersByBookId(UUID bookId, String sortBy, String sortDir) {
		Sort sort = createSort(sortBy, sortDir);
		return chapterRepo.findByBookId(bookId, sort);
	}

	@Override
	public List<Chapter> findNotDraftedChaptersByBookId(UUID bookId) {
		return chapterRepo.findNotDraftedChaptersByBookId(bookId);
	}

	@Override
	public List<Chapter> findNotDraftedChaptersByBookId(UUID bookId, String sortBy, String sortDir) {
		Sort sort = createSort(sortBy, sortDir);
		return chapterRepo.findNotDraftedChaptersByBookId(bookId, sort);
	}

	private Sort createSort(String sortBy, String sortDir) {
		String field = switch (sortBy != null ? sortBy.toLowerCase() : "uploaddate") {
			case "title", "name" -> "title";
			case "chapternum", "number" -> "chapterNum";
			case "price" -> "price";
			default -> "uploadDate";
		};
		Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, field);
	}

	@Override
	public List<Chapter> getAllChapters() {
		return chapterRepo.findAll();
	}

	@Override
	public Chapter createDraftChapter(UUID bookId, Chapter chapter) {
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
	public Chapter publishChapter(UUID bookId, Chapter chapter) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		chapter.setBook(book);
		chapter.setComments(new ArrayList<>());
		chapter.setDraft(false);
		if (chapter.getRoomId() == null) {
			chapter.setRoomId(UUID.randomUUID().toString());
		}
		chapter.setUploadDate(LocalDateTime.now());
		List<BookFavourite> bookFavourites = bookFavouriteRepository.findByBookId(bookId);
		String message = "New chapter added to " + book.getTitle() + ": " + chapter.getTitle();
		for (BookFavourite favourite : bookFavourites) {
			notificationService.createNotification(favourite.getUser(), message, NotificationEntityType.BOOK, bookId);
		}
		return chapterRepo.save(chapter);
	}

	@Override
	public Chapter editChapter(UUID chapterId, Chapter chapter) throws Exception {
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
	public String deleteChapter(UUID chapterId) throws Exception {
		Chapter deleteChapter = findChapterById(chapterId);
		if (deleteChapter == null) {
			throw new Exception("Chapter not found");
		}

		try {

			deleteChapter.setBook(null);

			progressRepo.deleteByChapterId(chapterId);

			List<User> users = deleteChapter.getLikedUsers();
			for (User user : users) {
				user.getLikedChapters().remove(deleteChapter);
			}
			deleteChapter.getLikedUsers().clear();

			List<Report> reports = reportRepository.findByChapterId(chapterId);
			for (Report report : reports) {
				report.setChapter(null);
				reportRepository.save(report);
			}

			// Handle author earnings - set chapter and unlockRecord references to null
			// to preserve earnings history
			List<AuthorEarning> earnings = authorEarningRepository.findByChapterId(chapterId);
			for (AuthorEarning earning : earnings) {
				earning.setChapter(null);
				earning.setUnlockRecord(null);
				authorEarningRepository.save(earning);
			}

			// Delete unlock records after removing references from author earnings
			List<ChapterUnlockRecord> unlockRecords = unlockRecordRepository.findByChapterId(chapterId);
			unlockRecordRepository.deleteAll(unlockRecords);

			chapterRepo.delete(deleteChapter);

			return "Chapter deleted successfully!";
		} catch (Exception e) {
			logger.error("Error deleting chapter: {}", e);
			throw new Exception("Error deleting chapter: " + e.getMessage(), e);
		}
	}

	@Override
	public void unlockChapter(UUID userId, UUID chapterId) throws Exception {
		Chapter chapter = chapterRepo.findById(chapterId).orElseThrow(() -> new Exception("Chapter not found"));

		if (!chapter.isLocked()) {
			throw new Exception("Chapter is not locked");
		}

		Optional<ChapterUnlockRecord> existingUnlock = unlockRecordRepository.findByUserIdAndChapterId(userId,
				chapterId);
		if (existingUnlock.isPresent()) {
			throw new Exception("Chapter already unlocked");
		}

		int unlockCost = chapter.getPrice();
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			userWalletService.deductCredits(userId, unlockCost);
		} catch (IllegalStateException e) {
			throw new Exception("Insufficient credits");
		}

		ChapterUnlockRecord unlockRecord = new ChapterUnlockRecord();
		unlockRecord.setUser(user);
		unlockRecord.setChapter(chapter);
		unlockRecord.setUnlockDate(LocalDateTime.now());
		unlockRecord.setUnlockCost(unlockCost);

		unlockRecord = unlockRecordRepository.save(unlockRecord);

		try {
			authorService.recordChapterUnlockEarning(unlockRecord);
			String message = "User " + user.getUsername() + " unlocked chapter " + chapter.getTitle() + ": "
					+ chapter.getChapterNum() + " in " + chapter.getBook().getTitle();
			notificationService.createNotification(chapter.getBook().getAuthor(), message,
					NotificationEntityType.CHAPTER, chapterId);
		} catch (Exception e) {
			logger.error("Error recording author earnings: {}", e);
		}

	}

	@Override
	public boolean isChapterUnlockedByUser(UUID userId, UUID chapterId) {
		Optional<ChapterUnlockRecord> unlockRecord = unlockRecordRepository.findByUserIdAndChapterId(userId, chapterId);
		return unlockRecord.isPresent();
	}

	@Override
	public Chapter getChapterByRoomId(String roomId) {
		return chapterRepo.findByRoomId(roomId).get();
	}

	@Override
	@Transactional
	public Boolean likeChapter(UUID userId, UUID chapterId) throws Exception {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		Chapter chapter = chapterRepo.findById(chapterId)
				.orElseThrow(() -> new ResourceNotFoundException("Chapter not found with ID: " + chapterId));

		if (!user.getLikedChapters().contains(chapter)) {
			user.getLikedChapters().add(chapter);
			chapter.getLikedUsers().add(user);
			userRepository.save(user);
			chapterRepo.save(chapter);

			String message = "User liked chapter " + chapter.getTitle() + ": " + chapter.getChapterNum() + " in "
					+ chapter.getBook().getTitle();
			notificationService.createNotification(chapter.getBook().getAuthor(), message,
					NotificationEntityType.CHAPTER, chapterId);

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
	public Chapter unlikeChapter(UUID userId, UUID chapterId) throws Exception {
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
	public boolean isChapterLikedByUser(UUID userId, UUID chapterId) {
		Optional<User> userOpt = userRepository.findById(userId);
		Optional<Chapter> chapterOpt = chapterRepo.findById(chapterId);
		if (userOpt.isPresent() && chapterOpt.isPresent()) {
			return userOpt.get().getLikedChapters().contains(chapterOpt.get());
		}
		return false;
	}

	@Override
	public void processChaptersByEpubFile(UUID bookId, InputStream inputStream, Integer startByChapterNum)
			throws IOException {
		try {
			EpubReader epubReader = new EpubReader();
			nl.siegmann.epublib.domain.Book book = epubReader.readEpub(inputStream);
			TableOfContents tableOfContents = book.getTableOfContents();

			if (tableOfContents == null || tableOfContents.getTocReferences() == null) {
				throw new IllegalStateException("Table of contents is missing or invalid in the EPUB file.");
			}

			List<TOCReference> tocReferences = tableOfContents.getTocReferences();

			int chapterNum = startByChapterNum;
			for (TOCReference tocReference : tocReferences) {
				if (tocReference == null || tocReference.getResource() == null) {
					// Log warning and skip invalid TOC reference
					continue;
				}

				Resource chapterResource = tocReference.getResource();
				String chapterTitle = chapterResource.getTitle() != null ? chapterResource.getTitle()
						: "Untitled Chapter";

				byte[] contentBytes = chapterResource.getData();
				String htmlContent = new String(contentBytes, StandardCharsets.UTF_8);

				Chapter chapter = new Chapter();
				chapter.setTitle(chapterTitle);
				chapter.setChapterNum(String.valueOf(chapterNum));
				chapter.setContent(htmlContent);

				try {
					createDraftChapter(bookId, chapter);
				} catch (Exception e) {
					// Log and handle specific exceptions from createDraftChapter
					throw new RuntimeException("Failed to create draft chapter for: " + chapterTitle, e);
				}
				chapterNum++;
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Error processing EPUB file for book ID: " + bookId, e);
		}
	}

	@Override
	public void processChaptersByDocFile(UUID bookId, InputStream inputStream, Integer startByChapterNum)
			throws IOException {
		// TODO Auto-generated method stub

	}

}
