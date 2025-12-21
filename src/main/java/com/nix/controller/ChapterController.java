package com.nix.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nix.dtos.BookDTO;
import com.nix.dtos.ChapterDTO;
import com.nix.dtos.ChapterSummaryDTO;
import com.nix.dtos.mappers.ChapterMapper;
import com.nix.dtos.mappers.ChapterSummaryMapper;
import com.nix.exception.ForbiddenAccessException;
import com.nix.exception.ResourceNotFoundException;
import com.nix.exception.UnauthorizedException;
import com.nix.models.Chapter;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.PaymentService;
import com.nix.service.ReadingProgressService;
import com.nix.service.UserService;
import com.nix.util.SecurityUtils;

@RestController
public class ChapterController {

	@Autowired
	ChapterService chapterService;

	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	ReadingProgressService progressService;

	@Autowired
	PaymentService paymentService;

	ChapterMapper chapterMapper = new ChapterMapper();

	ChapterSummaryMapper chapterSummaryMapper = new ChapterSummaryMapper();

	private boolean isAdmin(User user) {
		return SecurityUtils.isAdmin(user);
	}

	private User resolveCurrentUser(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			return null;
		}
		return userService.findUserByJwt(jwt);
	}

	private void ensureUserCanPublish(User user) {
		if (user == null) {
			throw new ResourceNotFoundException("Cannot find user");
		}
		if (user.isBanned()) {
			throw new UnauthorizedException("Your account is banned. Contact support for assistance.");
		}
		if (Boolean.TRUE.equals(user.getIsSuspended())) {
			throw new UnauthorizedException("Your account is suspended. Contact support for assistance.");
		}
		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			throw new UnauthorizedException("Please verify your account before modifying chapters.");
		}
	}

	private void ensureAuthorOrAdmin(User user, UUID ownerId) {
		if (user == null) {
			throw new UnauthorizedException("User has not logged in!");
		}
		if (isAdmin(user)) {
			return;
		}
		if (ownerId == null || !ownerId.equals(user.getId())) {
			throw new ForbiddenAccessException("Only the book author can modify this resource.");
		}
	}

	private void ensureNotBlocked(User user, UUID ownerId) {
		if (user == null || ownerId == null) {
			return;
		}
		if (isAdmin(user)) {
			return;
		}
		if (userService.isBlockedBy(user.getId(), ownerId) || userService.hasBlocked(user.getId(), ownerId)) {
			throw new ForbiddenAccessException(
					"You cannot access this resource because one of the accounts has blocked the other.");
		}
	}

	@GetMapping("/chapters")
	public ResponseEntity<ApiResponseWithData<List<ChapterDTO>>> getAllChapters() {
		List<Chapter> chapters = chapterService.getAllChapters();
		List<ChapterDTO> chapterDTOs = chapterMapper.mapToDTOs(chapters);

		ApiResponseWithData<List<ChapterDTO>> response = new ApiResponseWithData<>(
				"Chapters retrieved successfully.", true, chapterDTOs);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/books/{bookId}/chapters")
	public ResponseEntity<ApiResponseWithData<List<ChapterSummaryDTO>>> getAllChaptersByBookId(
			@PathVariable("bookId") UUID bookId,
			@RequestParam(value = "sortBy", defaultValue = "uploadDate") String sortBy,
			@RequestParam(value = "sortDir", defaultValue = "asc") String sortDir,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		if (currentUser != null) {
			BookDTO bookDTO = bookService.getBookById(bookId);
			UUID ownerId = bookDTO.getAuthor() != null ? bookDTO.getAuthor().getId() : null;
			ensureNotBlocked(currentUser, ownerId);
		}

		List<Chapter> chapters = chapterService.findNotDraftedChaptersByBookId(bookId, sortBy, sortDir);
		List<ChapterSummaryDTO> chapterDTOs = chapterSummaryMapper.mapToDTOs(chapters);

		boolean isAuthenticated = currentUser != null;

		for (int i = 0; i < chapters.size(); i++) {
			Chapter chapter = chapters.get(i);
			ChapterSummaryDTO dto = chapterDTOs.get(i);

			// If authenticated, check if the chapter is unlocked by the user
			if (isAuthenticated) {
				boolean isUnlocked = chapterService.isChapterUnlockedByUser(currentUser.getId(), chapter.getId());
				dto.setUnlockedByUser(isUnlocked);
			} else {
				dto.setUnlockedByUser(false);
			}
		}

		ApiResponseWithData<List<ChapterSummaryDTO>> response = new ApiResponseWithData<>(
				"Chapters retrieved successfully.", true, chapterDTOs);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<ApiResponseWithData<List<ChapterDTO>>> manageChaptersByBookId(
			@PathVariable("bookId") UUID bookId,
			@RequestParam(value = "sortBy", defaultValue = "uploadDate") String sortBy,
			@RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
		List<Chapter> chapters = chapterService.findChaptersByBookId(bookId, sortBy, sortDir);
		List<ChapterDTO> chapterDTOs = chapterMapper.mapToDTOs(chapters);
		ApiResponseWithData<List<ChapterDTO>> response = new ApiResponseWithData<>(
				"Chapters retrieved successfully.", true, chapterDTOs);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/chapters/{chapterId}")
	public ResponseEntity<ApiResponseWithData<?>> getChapterById(@PathVariable("chapterId") UUID chapterId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Chapter chapter = chapterService.findChapterById(chapterId);
		ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);
		boolean isUnlocked = false;
		boolean isLiked = false;

		User currentUser = resolveCurrentUser(jwt);
		if (currentUser != null) {
			UUID ownerId = chapter.getBook() != null && chapter.getBook().getAuthor() != null
					? chapter.getBook().getAuthor().getId()
					: null;
			ensureNotBlocked(currentUser, ownerId);
			isUnlocked = chapterService.isChapterUnlockedByUser(currentUser.getId(), chapterId);
			isLiked = chapterService.isChapterLikedByUser(currentUser.getId(), chapterId);
			chapterDTO.setUnlockedByUser(isUnlocked);
			chapterDTO.setLikedByCurrentUser(isLiked);
		}

		if (chapter.getPrice() > 0 && chapter.isLocked() && !isUnlocked) {
			ChapterSummaryDTO summary = chapterSummaryMapper.mapToDTO(chapter);
			return ResponseEntity
					.ok(new ApiResponseWithData<>("Chapter summary retrieved successfully.", true, summary));
		}

		return ResponseEntity.ok(new ApiResponseWithData<>("Chapter retrieved successfully.", true, chapterDTO));
	}

	@GetMapping("/api/chapters/room/{roomId}")
	public ResponseEntity<ApiResponseWithData<ChapterDTO>> getChapterByRoomId(@PathVariable String roomId) {
		Chapter chapter = chapterService.getChapterByRoomId(roomId);
		ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);
		return ResponseEntity.ok(new ApiResponseWithData<>("Chapter retrieved successfully.", true, chapterDTO));
	}

	@PostMapping("/api/books/{bookId}/chapters/upload/epub")
	public ResponseEntity<ApiResponseWithData<String>> processChaptersFromEpub(@PathVariable("bookId") UUID bookId,
			@RequestParam("file") MultipartFile file, @RequestParam Integer startChapterNum,
			@RequestHeader("Authorization") String jwt) throws Exception {
		User currentUser = userService.findUserByJwt(jwt);
		ensureUserCanPublish(currentUser);
		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>("Book ID not found!", false));
		}
		UUID ownerId = book.getAuthor() != null ? book.getAuthor().getId() : null;
		ensureNotBlocked(currentUser, ownerId);
		ensureAuthorOrAdmin(currentUser, ownerId);

		try {
			chapterService.processChaptersByEpubFile(bookId, file.getInputStream(), startChapterNum);
			return ResponseEntity.ok(new ApiResponseWithData<>("EPUB processed successfully!", true, "success"));
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error processing EPUB: " + e.getMessage(), false));
		}
	}

	@PostMapping("/api/books/{bookId}/chapters/draft")
	public ResponseEntity<ApiResponseWithData<Chapter>> createDraftChapter(@PathVariable("bookId") UUID bookId,
			@RequestBody Chapter chapter,
			@RequestHeader("Authorization") String jwt) throws Exception {
		User currentUser = userService.findUserByJwt(jwt);
		ensureUserCanPublish(currentUser);
		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>("Book ID not found!", false));
		}
		UUID ownerId = book.getAuthor() != null ? book.getAuthor().getId() : null;
		ensureNotBlocked(currentUser, ownerId);
		ensureAuthorOrAdmin(currentUser, ownerId);

		try {
			Chapter newChapter = chapterService.createDraftChapter(bookId, chapter);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(new ApiResponseWithData<>("Draft chapter created successfully.", true, newChapter));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error creating draft chapter: " + e.getMessage(), false));
		}
	}

	@PostMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<ApiResponseWithData<Chapter>> publishChapter(@PathVariable("bookId") UUID bookId,
			@RequestBody Chapter chapter,
			@RequestHeader("Authorization") String jwt) throws Exception {
		User currentUser = userService.findUserByJwt(jwt);
		ensureUserCanPublish(currentUser);
		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>("Book ID not found!", false));
		}
		UUID ownerId = book.getAuthor() != null ? book.getAuthor().getId() : null;
		ensureNotBlocked(currentUser, ownerId);
		ensureAuthorOrAdmin(currentUser, ownerId);

		try {
			Chapter newChapter = chapterService.publishChapter(bookId, chapter);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(new ApiResponseWithData<>("Chapter published successfully.", true, newChapter));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error publishing chapter: " + e.getMessage(), false));
		}
	}

	@PutMapping("/api/chapters/{chapterId}")
	public ResponseEntity<ApiResponseWithData<ChapterDTO>> editChapter(@PathVariable("chapterId") UUID chapterId,
			@RequestBody Chapter chapter, @RequestHeader("Authorization") String jwt) throws Exception {

		User currentUser = userService.findUserByJwt(jwt);
		ensureUserCanPublish(currentUser);
		Chapter existingChapter = chapterService.findChapterById(chapterId);
		UUID ownerId = existingChapter.getBook() != null && existingChapter.getBook().getAuthor() != null
				? existingChapter.getBook().getAuthor().getId()
				: null;
		ensureNotBlocked(currentUser, ownerId);
		ensureAuthorOrAdmin(currentUser, ownerId);

		Chapter editChapter = chapterService.editChapter(chapterId, chapter);
		ChapterDTO chapterDTO = chapterMapper.mapToDTO(editChapter);
		return ResponseEntity.ok(new ApiResponseWithData<>("Chapter updated successfully.", true, chapterDTO));
	}

	@DeleteMapping("/api/chapters/{chapterId}")
	public ResponseEntity<ApiResponseWithData<Void>> deleteChapter(@PathVariable("chapterId") UUID chapterId,
			@RequestHeader("Authorization") String jwt) throws Exception {

		User currentUser = userService.findUserByJwt(jwt);
		if (currentUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ApiResponseWithData<>("User has not logged in!", false));
		}

		Chapter chapter = chapterService.findChapterById(chapterId);
		UUID ownerId = chapter.getBook() != null && chapter.getBook().getAuthor() != null
				? chapter.getBook().getAuthor().getId()
				: null;
		boolean isAdmin = isAdmin(currentUser);
		if (!isAdmin && (ownerId == null || !currentUser.getId().equals(ownerId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"You do not have permission to delete this chapter.", false));
		}

		String resultMessage = chapterService.deleteChapter(chapterId);
		ApiResponseWithData<Void> response = new ApiResponseWithData<>(resultMessage, true);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@PostMapping("/api/unlock/{chapterId}")
	public ResponseEntity<ApiResponseWithData<String>> unlockChapter(@PathVariable("chapterId") UUID chapterId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			Chapter chapter = chapterService.findChapterById(chapterId);
			UUID ownerId = chapter.getBook() != null && chapter.getBook().getAuthor() != null
					? chapter.getBook().getAuthor().getId()
					: null;
			ensureNotBlocked(user, ownerId);
			chapterService.unlockChapter(user.getId(), chapterId);
			return ResponseEntity.ok(new ApiResponseWithData<>("Chapter unlocked successfully.", true, "unlocked"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PutMapping("/api/chapters/{chapterId}/like")
	public ResponseEntity<ApiResponseWithData<ChapterDTO>> likeChapter(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID chapterId) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponseWithData<>("User has not logged in!", false));
			}
			Chapter chapter = chapterService.findChapterById(chapterId);
			UUID ownerId = chapter.getBook() != null && chapter.getBook().getAuthor() != null
					? chapter.getBook().getAuthor().getId()
					: null;
			ensureNotBlocked(user, ownerId);

			Boolean isLiked = chapterService.likeChapter(user.getId(), chapterId);
			boolean isUnlocked = chapterService.isChapterUnlockedByUser(user.getId(), chapterId);
			ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);
			chapterDTO.setLikedByCurrentUser(isLiked);
			chapterDTO.setUnlockedByUser(isUnlocked);

			String message = isLiked ? "Chapter liked successfully." : "Chapter unliked successfully.";
			return ResponseEntity.ok(new ApiResponseWithData<>(message, true, chapterDTO));

		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(ex.getMessage(), false));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error liking chapter.", false));
		}
	}

	/**
	 * Endpoint to check if a chapter is liked by the user.
	 */
	@GetMapping("/api/chapters/{chapterId}/isLiked")
	public ResponseEntity<ApiResponseWithData<Boolean>> isChapterLiked(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID chapterId) {
		try {
			User user = userService.findUserByJwt(jwt);
			Chapter chapter = chapterService.findChapterById(chapterId);
			UUID ownerId = chapter.getBook() != null && chapter.getBook().getAuthor() != null
					? chapter.getBook().getAuthor().getId()
					: null;
			ensureNotBlocked(user, ownerId);
			boolean isLiked = chapterService.isChapterLikedByUser(user.getId(), chapterId);
			return ResponseEntity.ok(new ApiResponseWithData<>("Like status retrieved successfully.", true, isLiked));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponseWithData<>("Error retrieving like status.", false, false));
		}
	}

}
