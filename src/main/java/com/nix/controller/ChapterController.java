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

import com.nix.dtos.ChapterDTO;
import com.nix.dtos.ChapterSummaryDTO;
import com.nix.dtos.mappers.ChapterMapper;
import com.nix.dtos.mappers.ChapterSummaryMapper;
import com.nix.models.Book;
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
		User user = null;
		if (jwt != null) {
			user = userService.findUserByJwt(jwt);
		}
		List<Chapter> chapters = chapterService.findChaptersByBookIdWithUnlockStatus(bookId,
				user != null ? user.getId() : null);

		return ResponseEntity.ok(chapterSummaryMapper.mapToDTOs(chapters));
	}

	@GetMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<List<ChapterDTO>> manageChaptersByBookId(@PathVariable("bookId") Integer bookId) {
		List<Chapter> chapters = chapterService.findChaptersByBookId(bookId);

		return ResponseEntity.ok(chapterMapper.mapToDTOs(chapters));
	}

	@GetMapping("/books/{bookId}/chapters/{chapterId}")
	public ResponseEntity<?> getChapterById(@PathVariable("chapterId") Integer chapterId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Chapter chapter = chapterService.findChapterById(chapterId);
		ChapterDTO chapterDTO = chapterMapper.mapToDTO(chapter);

		if (jwt != null) {

			User user = userService.findUserByJwt(jwt);
			if (chapter.isLocked()&& !chapterService.isChapterUnlockedByUser(user.getId(), chapterId) && chapter.getPrice() > 0) {
				return ResponseEntity.ok(chapterSummaryMapper.mapToDTO(chapter));
			}
			if (user != null) {
				boolean isUnlocked = chapterService.isChapterUnlockedByUser(user.getId(), chapterId);
				System.out.print(isUnlocked);
				chapterDTO.setUnlockedByUser(isUnlocked);
			}
		}
		return ResponseEntity.ok(chapterDTO);
	}

	@PostMapping("/api/books/{bookId}/chapters")
	public ResponseEntity<Chapter> addNewChapter(@PathVariable("bookId") Integer bookId, @RequestBody Chapter chapter)
			throws Exception {
		Book book = bookService.getBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter newChapter = chapterService.addChapterAndNotifyFollowers(bookId, chapter);
		return new ResponseEntity<Chapter>(newChapter, HttpStatus.CREATED);
	}

	@PutMapping("/api/books/{bookId}/chapters/{chapterId}")
	public ResponseEntity<ChapterDTO> editChapter(@PathVariable("bookId") Integer bookId,
			@PathVariable("chapterId") Integer chapterId, @RequestBody Chapter chapter) throws Exception {
		Book book = bookService.getBookById(bookId);

		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter editChapter = chapterService.editChapter(chapterId, chapter);
		return ResponseEntity.ok(chapterMapper.mapToDTO(editChapter));
	}

	@DeleteMapping("/api/books/{bookId}/chapters/{chapterId}")
	public ResponseEntity<ApiResponse> deleteChapter(@PathVariable("bookId") Integer bookId,
			@PathVariable("chapterId") Integer chapterId) throws Exception {
		Chapter chapter = chapterService.findChapterById(chapterId);
		if (chapter.getBook().getId().equals(bookId)) {
			ApiResponse res = new ApiResponse(chapterService.deleteChapter(chapterId), true);
			return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
		} else {
			ApiResponse res = new ApiResponse("Chapter is not belonged to this book!", false);
			return new ResponseEntity<>(res, HttpStatus.NOT_ACCEPTABLE);
		}
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

}
