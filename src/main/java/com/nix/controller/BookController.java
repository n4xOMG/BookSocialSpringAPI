package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.NotificationService; // Add this import
import com.nix.service.UserService;

@RestController
public class BookController {

	@Autowired
	private BookService bookService;

	@Autowired
	private UserService userService;

	@Autowired
	private NotificationService notificationService;

	@GetMapping("/books")
	public ResponseEntity<List<BookDTO>> getAllBooks() {
		return ResponseEntity.ok(bookService.getAllBooks());
	}

	@GetMapping("/books/{bookId}")
	public ResponseEntity<BookDTO> getBookById(@PathVariable Integer bookId) {
		return ResponseEntity.ok(bookService.getBookById(bookId));
	}

	@GetMapping("/books/top-likes")
	public ResponseEntity<List<BookDTO>> getTop10BooksByLikes() {
		return ResponseEntity.ok(bookService.getTop10LikedBooks());
	}

	@GetMapping("/books/featured")
	public ResponseEntity<List<BookDTO>> getFeaturedBooks() {
		return ResponseEntity.ok(bookService.getFeaturedBooks());
	}

	@GetMapping("/books/{bookId}/related")
	public ResponseEntity<List<BookDTO>> getRelatedBooks(@PathVariable("bookId") Integer bookId,
			@RequestParam(value = "tagIds", required = false) List<Integer> tagIds) {
		return ResponseEntity.ok(bookService.getRelatedBooks(bookId, tagIds));
	}

	@GetMapping("/books/search")
	public ResponseEntity<List<BookDTO>> searchBooks(@RequestParam(required = false) String title,
			@RequestParam(required = false) Integer categoryId, @RequestParam(required = false) List<Integer> tagIds) {
		return ResponseEntity.ok(bookService.searchBooks(title, categoryId, tagIds));
	}

	@GetMapping("/books/author/{authorId}")
	public ResponseEntity<List<BookDTO>> getBooksByAuthor(@PathVariable Integer authorId) {
		return ResponseEntity.ok(bookService.getBooksByAuthor(authorId));
	}

	@GetMapping("/top-categories")
	public ResponseEntity<List<CategoryDTO>> getTopSixCategoriesWithBooks() {
		return ResponseEntity.ok(bookService.getTopSixCategoriesWithBooks());
	}

	@GetMapping("/api/books/favoured")
	public ResponseEntity<List<BookDTO>> getUserFavouredBooks(@RequestHeader("Authorization") String jwt) {
		User user = userService.findUserByJwt(jwt);
		return ResponseEntity.ok(bookService.getFollowedBooksByUserId(user.getId()));
	}

	@GetMapping("/books/latest-update")
	public ResponseEntity<List<BookDTO>> getLatestUpdateBooks(@RequestParam(defaultValue = "5") int limit) {
		return ResponseEntity.ok(bookService.getTopRecentChapterBooks(limit));
	}

	@GetMapping("/categories/{categoryId}/books")
	public ResponseEntity<List<BookDTO>> getBooksByCategory(@PathVariable Integer categoryId) {
		return ResponseEntity.ok(bookService.getBooksByCategoryId(categoryId));
	}

	@GetMapping("/api/books/{bookId}/isLiked")
	public ResponseEntity<Boolean> checkBookLikedByUser(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer bookId) {
		User user = userService.findUserByJwt(jwt);
		return ResponseEntity.ok(bookService.isBookLikedByUser(user.getId(), bookId));
	}

	@PostMapping("/api/books")
	public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
		BookDTO createdBook = bookService.createBook(bookDTO);
		Integer authorId = createdBook.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + createdBook.getTitle() + "' has been created!");
		}
		return ResponseEntity.ok(createdBook);
	}

	@PutMapping("/api/books/{bookId}")
	public ResponseEntity<BookDTO> updateBook(@PathVariable("bookId") Integer bookId, @RequestBody BookDTO bookDTO) {
		BookDTO updatedBook = bookService.updateBook(bookId, bookDTO);
		Integer authorId = updatedBook.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + updatedBook.getTitle() + "' has been updated!");
		}
		return ResponseEntity.ok(updatedBook);
	}

	@DeleteMapping("/api/books/{bookId}")
	public ResponseEntity<Void> deleteBook(@PathVariable("bookId") Integer bookId) {
		BookDTO book = bookService.getBookById(bookId);
		bookService.deleteBook(bookId);
		Integer authorId = book.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author, "Your book '" + book.getTitle() + "' has been deleted!");
		}
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/api/books/follow/{bookId}")
	public ResponseEntity<Boolean> markBookAsFavoured(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer bookId) {
		User reqUser = userService.findUserByJwt(jwt);
		boolean isFollowed = bookService.markAsFavouriteBook(bookService.getBookById(bookId), reqUser);
		return ResponseEntity.ok(isFollowed);
	}

	@PutMapping("/api/books/{bookId}/editor-choice")
	public ResponseEntity<BookDTO> setEditorChoice(@PathVariable Integer bookId, @RequestBody BookDTO bookDTO) {
		BookDTO updatedBook = bookService.setEditorChoice(bookId, bookDTO);
		Integer authorId = updatedBook.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + updatedBook.getTitle() + "' has been selected as Editor's Choice!");
		}
		return ResponseEntity.ok(updatedBook);
	}
}