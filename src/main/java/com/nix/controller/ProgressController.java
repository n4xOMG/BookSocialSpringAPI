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

import com.nix.dtos.ReadingProgressDTO;
import com.nix.dtos.mappers.ReadingProgressMapper;
import com.nix.models.Book;
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

	ReadingProgressMapper progressMapper = new ReadingProgressMapper();

	@GetMapping("/api/reading-progress/chapters/{chapterId}")
	public ResponseEntity<ReadingProgressDTO> getReadingProgressByUserAndChapter(
			@RequestHeader("Authorization") String jwt, @PathVariable("chapterId") Integer chapterId) throws Exception {

		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("The user have to log in!");
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter == null) {
			throw new Exception("Chapter not found");
		}
		ReadingProgress readingProgress = progressService.findByUserAndChapter(user.getId(), chapterId);
		if (readingProgress != null) {
			return new ResponseEntity<>(progressMapper.mapToDTO(readingProgress), HttpStatus.OK);
		}
		return null;

	}

	@GetMapping("/api/reading-progress/books/{bookId}")
	public ResponseEntity<?> getReadingProgressByUserAndBook(@RequestHeader("Authorization") String jwt,
			@PathVariable("bookId") Integer bookId) throws Exception {

		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				throw new Exception("The user have to log in!");
			}

			Book book = bookService.getBookById(bookId);
			if (book == null) {
				throw new Exception("Chapter not found");
			}
			List<ReadingProgress> readingProgresses = progressService.findByUserAndBook(user.getId(), bookId);
			return new ResponseEntity<>(progressMapper.mapToDTOs(readingProgresses), HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	@GetMapping("/api/reading-progress")
	public ResponseEntity<List<ReadingProgressDTO>> getReadingProgressByUser(@RequestHeader("Authorization") String jwt)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("The user have to log in!");
		}

		List<ReadingProgress> readingProgresses = progressService.findAllReadingProgressByUserId(user.getId());
		if (readingProgresses != null) {
			return new ResponseEntity<List<ReadingProgressDTO>>(progressMapper.mapToDTOs(readingProgresses),
					HttpStatus.OK);
		}
		return null;
	}

	@PostMapping("/api/chapters/{chapterId}/progress")
	public ResponseEntity<ReadingProgressDTO> saveReadingProgress(@RequestHeader("Authorization") String jwt,
			@PathVariable("chapterId") Integer chapterId, @RequestBody ReadingProgress progress) throws Exception {

		User reqUser = userService.findUserByJwt(jwt);
		if (reqUser == null) {
			throw new Exception("User not found");
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter == null) {
			throw new Exception("Chapter not found");
		}

		ReadingProgress readingProgress = progressService.findByUserAndChapter(reqUser.getId(), chapterId);

		if (readingProgress == null) {
			ReadingProgress newProgress = new ReadingProgress();
			newProgress.setUser(reqUser);
			newProgress.setChapter(chapter);
			newProgress.setProgress(progress.getProgress());
			return new ResponseEntity<>(progressMapper.mapToDTO(progressService.createReadingProgress(newProgress)),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<>(
					progressMapper.mapToDTO(progressService.updateReadingProgress(readingProgress.getId(), progress)),
					HttpStatus.OK);
		}

	}
}
