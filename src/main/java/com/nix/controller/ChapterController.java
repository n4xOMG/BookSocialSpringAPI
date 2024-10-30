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
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.ChapterDTO;
import com.nix.dtos.mappers.ChapterMapper;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.response.ApiResponse;
import com.nix.service.BookService;
import com.nix.service.ChapterService;
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

	ChapterMapper chapterMapper = new ChapterMapper();

	@GetMapping("/chapters")
	public ResponseEntity<List<ChapterDTO>> getAllChapters() {
		List<Chapter> chapters = chapterService.getAllChapters();

		return ResponseEntity.ok(chapterMapper.mapToDTOs(chapters));
	}

	@GetMapping("/books/{bookId}/chapters")
	public ResponseEntity<List<ChapterDTO>> getAllChaptersByBookId(@PathVariable("bookId") Integer bookId) {
		List<Chapter> chapters = chapterService.findChaptersByBookId(bookId);

		return ResponseEntity.ok(chapterMapper.mapToDTOs(chapters));
	}

	@GetMapping("/books/{bookId}/chapters/{chapterId}")
	public ResponseEntity<ChapterDTO> getChapterById(@PathVariable("chapterId") Integer chapterId) throws Exception {
		Chapter chapter = chapterService.findChapterDTOById(chapterId);
		return ResponseEntity.ok(chapterMapper.mapToDTO(chapter));
	}

	@PostMapping("/translator/books/{bookId}/chapters")
	public ResponseEntity<Chapter> addNewChapter(@PathVariable("bookId") Integer bookId, @RequestBody Chapter chapter)
			throws Exception {
		Book book = bookService.findBookById(bookId);
		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter newChapter = chapterService.addNewChapter(bookId, chapter);
		return new ResponseEntity<Chapter>(newChapter, HttpStatus.CREATED);
	}

	@PutMapping("/translator/books/{bookId}/chapters/{chapterId}")
	public ResponseEntity<ChapterDTO> editChapter(@PathVariable("bookId") Integer bookId,
			@PathVariable("chapterId") Integer chapterId, @RequestBody Chapter chapter) throws Exception {
		Book book = bookService.findBookById(bookId);

		if (book == null) {
			throw new Exception("Book not found");
		}

		Chapter editChapter = chapterService.editChapter(chapterId, chapter);
		return ResponseEntity.ok(chapterMapper.mapToDTO(editChapter));
	}

	@DeleteMapping("/translator/books/{bookId}/chapters/{chapterId}")
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

}
