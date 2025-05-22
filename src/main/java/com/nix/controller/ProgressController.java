package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.ReadingProgressDTO;
import com.nix.models.Chapter;
import com.nix.models.ReadingProgress;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.ReadingProgressService;
import com.nix.service.UserService;

@RestController
public class ProgressController {
	@Autowired
	ChapterService chapterService;

	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	ReadingProgressService progressService;

	@GetMapping("/api/reading-progress/chapters/{chapterId}")
	public ResponseEntity<ReadingProgressDTO> getReadingProgressByUserAndChapter(
			@RequestHeader("Authorization") String jwt, @PathVariable("chapterId") Long chapterId) throws Exception {

		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("The user must log in!");
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter == null) {
			throw new Exception("Chapter not found");
		}

		ReadingProgressDTO readingProgress = progressService.findByUserAndChapter(user.getId(), chapterId);
		return readingProgress != null ? ResponseEntity.ok(readingProgress) : ResponseEntity.ok(null);
	}

	@GetMapping("/api/reading-progress/books/{bookId}")
	public ResponseEntity<?> getReadingProgressByUserAndBook(@RequestHeader("Authorization") String jwt,
			@PathVariable("bookId") Long bookId) throws Exception {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				throw new Exception("The user must log in!");
			}

			BookDTO book = bookService.getBookById(bookId);
			if (book == null) {
				throw new Exception("Book not found");
			}

			List<ReadingProgressDTO> readingProgresses = progressService.findByUserAndBook(user.getId(), bookId);
			return ResponseEntity.ok(readingProgresses);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/api/reading-progress")
	public ResponseEntity<List<ReadingProgressDTO>> getReadingProgressByUser(@RequestHeader("Authorization") String jwt)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("The user must log in!");
		}

		List<ReadingProgressDTO> readingProgresses = progressService.findAllReadingProgressByUserId(user.getId());
		return ResponseEntity.ok(readingProgresses);
	}

	@PostMapping("/api/chapters/{chapterId}/progress")
	public ResponseEntity<ReadingProgressDTO> saveReadingProgress(@RequestHeader("Authorization") String jwt,
			@PathVariable("chapterId") Long chapterId, @RequestBody ReadingProgressDTO progressDTO)
			throws Exception {

		User reqUser = userService.findUserByJwt(jwt);
		if (reqUser == null) {
			throw new Exception("User not found");
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter == null) {
			throw new Exception("Chapter not found");
		}

		ReadingProgressDTO existingProgress = progressService.findByUserAndChapter(reqUser.getId(), chapterId);

		if (existingProgress == null) {
			ReadingProgress newProgress = new ReadingProgress();
			newProgress.setUser(reqUser);
			newProgress.setChapter(chapter);
			newProgress.setProgress(progressDTO.getProgress());
			return ResponseEntity.ok(progressService.createReadingProgress(newProgress));
		} else {
			ReadingProgress updateProgress = new ReadingProgress();
			updateProgress.setProgress(progressDTO.getProgress());
			return ResponseEntity.ok(progressService.updateReadingProgress(existingProgress.getId(), updateProgress));
		}
	}
}
