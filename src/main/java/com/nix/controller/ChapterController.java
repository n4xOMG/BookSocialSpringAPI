package com.nix.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.BookDTO;
import com.nix.dtos.ChapterDTO;
import com.nix.dtos.ChapterSummaryDTO;
import com.nix.dtos.mappers.ChapterMapper;
import com.nix.dtos.mappers.ChapterSummaryMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Chapter;
import com.nix.models.User;
import com.nix.response.ApiResponse;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
import com.nix.service.PaymentService;
import com.nix.service.ReadingProgressService;
import com.nix.service.UserService;

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

	@GetMapping("/chapters")
	public ResponseEntity<List<ChapterDTO>> getAllChapters() {
		List<Chapter> chapters = chapterService.getAllChapters();

		return ResponseEntity.ok(chapterMapper.mapToDTOs(chapters));
	}

	@GetMapping("/books/{bookId}/chapters")
	public ResponseEntity<List<ChapterSummaryDTO>> getAllChaptersByBookId(@PathVariable("bookId") Integer bookId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		List<Chapter> chapters = chapterService.findNotDraftedChaptersByBookId(bookId);
		List<ChapterSummaryDTO> chapterDTOs = chapterSummaryMapper.mapToDTOs(chapters);

		// Initialize the unlock status to false
		boolean isAuthenticated = jwt != null;
		User user = null;
		if (isAuthenticated) {
			// Check if the user exists
			user = userService.findUserByJwt(jwt);
		}

		for (int i = 0; i < chapters.size(); i++) {
			Chapter chapter = chapters.get(i);
			ChapterSummaryDTO dto = chapterDTOs.get(i);

			// If authenticated, check if the chapter is unlocked by the user
			if (isAuthenticated && user != null) {
				boolean isUnlocked = chapterService.isChapterUnlockedByUser(user.getId(), chapter.getId());
				dto.setUnlockedByUser(isUnlocked);
			} else {
				// Default to false if the user is not authenticated
				dto.setUnlockedByUser(false);
			}
		}

		return ResponseEntity.ok(chapterDTOs);
	}

	@GetMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<List<ChapterDTO>> manageChaptersByBookId(@PathVariable("bookId") Integer bookId) {
		List<Chapter> chapters = chapterService.findChaptersByBookId(bookId);

		return ResponseEntity.ok(chapterMapper.mapToDTOs(chapters));
	}

	@GetMapping("/chapters/{chapterId}")
	public ResponseEntity<?> getChapterById(@PathVariable("chapterId") Integer chapterId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Chapter chapter = chapterService.findChapterById(chapterId);
		ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);
		boolean isUnlocked = false;
		boolean isLiked = false;

		// Check JWT and retrieve user if available
		if (jwt != null) {
			User user = userService.findUserByJwt(jwt);
			isUnlocked = chapterService.isChapterUnlockedByUser(user.getId(), chapterId);
			isLiked = chapterService.isChapterLikedByUser(user.getId(), chapterId);
			chapterDTO.setUnlockedByUser(isUnlocked);
			chapterDTO.setLikedByCurrentUser(isLiked);
		}

		// For locked chapters with a price, return the summary if not unlocked
		if (chapter.getPrice() > 0 && chapter.isLocked() && !isUnlocked) {
			return ResponseEntity.ok(chapterSummaryMapper.mapToDTO(chapter));
		}

		// Return the full DTO for free or unlocked chapters
		return ResponseEntity.ok(chapterDTO);
	}

	@GetMapping("/api/chapters/room/{roomId}")
	public ResponseEntity<ChapterDTO> getChapterByRoomId(@PathVariable String roomId) {
		Chapter chapter = chapterService.getChapterByRoomId(roomId);
		return ResponseEntity.ok(chapterMapper.mapToDTO(chapter));
	}

	@PostMapping("/api/books/{bookId}/chapters/draft")
	public ResponseEntity<Chapter> createDraftChapter(@PathVariable("bookId") Integer bookId,
			@RequestBody Chapter chapter) throws Exception {
		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter newChapter = chapterService.createDraftChapter(bookId, chapter);
		return new ResponseEntity<Chapter>(newChapter, HttpStatus.CREATED);
	}

	@PostMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<Chapter> publishChapter(@PathVariable("bookId") Integer bookId, @RequestBody Chapter chapter)
			throws Exception {
		BookDTO book = bookService.getBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter newChapter = chapterService.publishChapter(bookId, chapter);
		return new ResponseEntity<Chapter>(newChapter, HttpStatus.CREATED);
	}

	@PutMapping("/api/chapters/{chapterId}")
	public ResponseEntity<ChapterDTO> editChapter(@PathVariable("chapterId") Integer chapterId,
			@RequestBody Chapter chapter) throws Exception {

		Chapter editChapter = chapterService.editChapter(chapterId, chapter);
		return ResponseEntity.ok(chapterMapper.mapToDTO(editChapter));
	}

	@DeleteMapping("/api/chapters/{chapterId}")
	public ResponseEntity<ApiResponse> deleteChapter(@PathVariable("chapterId") Integer chapterId) throws Exception {
		Chapter chapter = chapterService.findChapterById(chapterId);
		ApiResponse res = new ApiResponse(chapterService.deleteChapter(chapterId), true);
		return new ResponseEntity<>(res, HttpStatus.OK);
	}

	@PostMapping("/api/unlock/{chapterId}")
	public ResponseEntity<?> unlockChapter(@PathVariable("chapterId") Integer chapterId,
			@RequestHeader("Authorization") String jwt) {
		try {
			User user = userService.findUserByJwt(jwt);
			chapterService.unlockChapter(user.getId(), chapterId);
			return ResponseEntity.ok("Chapter unlocked successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@PutMapping("/api/chapters/{chapterId}/like")
	public ResponseEntity<?> likeChapter(@RequestHeader("Authorization") String jwt, @PathVariable Integer chapterId) {
		try {
			User user = userService.findUserByJwt(jwt);
			if (user == null) {
				return new ResponseEntity<>("User has not logged in!", HttpStatus.UNAUTHORIZED);
			}

			Boolean isLiked = chapterService.likeChapter(user.getId(), chapterId);
			Chapter chapter = chapterService.findChapterById(chapterId);
			ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);
			chapterDTO.setLikedByCurrentUser(isLiked);

			return ResponseEntity.ok(chapterDTO);

		} catch (ResourceNotFoundException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error liking chapter.");
		}
	}

	/**
	 * Endpoint to check if a chapter is liked by the user.
	 */
	@GetMapping("/api/chapters/{chapterId}/isLiked")
	public ResponseEntity<Boolean> isChapterLiked(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer chapterId) {
		try {
			User user = userService.findUserByJwt(jwt);
			boolean isLiked = chapterService.isChapterLikedByUser(user.getId(), chapterId);
			return ResponseEntity.ok(isLiked);
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
		}
	}

}
