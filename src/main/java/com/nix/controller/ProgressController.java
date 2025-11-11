package com.nix.controller;

import java.util.List;
import java.util.UUID;

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
import com.nix.response.ApiResponseWithData;
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
	public ResponseEntity<ApiResponseWithData<ReadingProgressDTO>> getReadingProgressByUserAndChapter(
			@RequestHeader("Authorization") String jwt, @PathVariable("chapterId") UUID chapterId) {

		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "The user must log in!");
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter == null) {
			return buildErrorResponse(HttpStatus.NOT_FOUND, "Chapter not found");
		}

		ReadingProgressDTO readingProgress = progressService.findByUserAndChapter(user.getId(), chapterId);
		return buildSuccessResponse("Reading progress retrieved successfully.", readingProgress);
	}

	@GetMapping("/api/reading-progress/books/{bookId}")
	public ResponseEntity<ApiResponseWithData<List<ReadingProgressDTO>>> getReadingProgressByUserAndBook(
			@RequestHeader("Authorization") String jwt,
			@PathVariable("bookId") UUID bookId) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "The user must log in!");
			}

			BookDTO book = bookService.getBookById(bookId);
			if (book == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Book not found");
			}

			List<ReadingProgressDTO> readingProgresses = progressService.findByUserAndBook(user.getId(), bookId);
			return buildSuccessResponse("Reading progress retrieved successfully.", readingProgresses);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping("/api/reading-progress")
	public ResponseEntity<ApiResponseWithData<List<ReadingProgressDTO>>> getReadingProgressByUser(
			@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "The user must log in!");
		}

		List<ReadingProgressDTO> readingProgresses = progressService.findAllReadingProgressByUserId(user.getId());
		return buildSuccessResponse("Reading progress retrieved successfully.", readingProgresses);
	}

	@PostMapping("/api/chapters/{chapterId}/progress")
	public ResponseEntity<ApiResponseWithData<ReadingProgressDTO>> saveReadingProgress(
			@RequestHeader("Authorization") String jwt,
			@PathVariable("chapterId") UUID chapterId, @RequestBody ReadingProgressDTO progressDTO) {
		try {
			User reqUser = userService.findUserByJwt(jwt);
			if (reqUser == null) {
				return buildErrorResponse(HttpStatus.UNAUTHORIZED, "User not found");
			}

			Chapter chapter = chapterService.findChapterById(chapterId);
			if (chapter == null) {
				return buildErrorResponse(HttpStatus.NOT_FOUND, "Chapter not found");
			}

			ReadingProgressDTO existingProgress = progressService.findByUserAndChapter(reqUser.getId(), chapterId);

			if (existingProgress == null) {
				ReadingProgress newProgress = new ReadingProgress();
				newProgress.setUser(reqUser);
				newProgress.setChapter(chapter);
				newProgress.setProgress(progressDTO.getProgress());
				return buildSuccessResponse(HttpStatus.CREATED, "Reading progress saved successfully.",
						progressService.createReadingProgress(newProgress));
			} else {
				ReadingProgress updateProgress = new ReadingProgress();
				updateProgress.setProgress(progressDTO.getProgress());
				return buildSuccessResponse("Reading progress updated successfully.",
						progressService.updateReadingProgress(existingProgress.getId(), updateProgress));
			}
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return ResponseEntity.ok(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}
