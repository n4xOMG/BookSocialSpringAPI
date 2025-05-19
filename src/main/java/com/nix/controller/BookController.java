package com.nix.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.models.Book;
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
	public ResponseEntity<Page<BookDTO>> getAllBooks(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		Page<BookDTO> booksPage = bookService.getAllBooks(pageable);

		boolean isAuthenticated = jwt != null;
		if (isAuthenticated) {
			User user = userService.findUserByJwt(jwt);
			if (user != null) {
				// Get the list of followed book IDs in a single query
				List<Integer> followedBookIds = user.getFollowedBooks().stream().map(Book::getId)
						.collect(Collectors.toList());

				booksPage.getContent().forEach(bookDTO -> {
					bookDTO.setFollowedByCurrentUser(followedBookIds.contains(bookDTO.getId()));
				});
			}
		}

		return ResponseEntity.ok(booksPage);
	}

	@GetMapping("/books/books-upload-per-month")
	public ResponseEntity<List<Long>> getBooksUploadedPerMonthCount() {
		return ResponseEntity.ok(bookService.getBookUploadedPerMonthNumber());
	}

	@GetMapping("/books/author/{authorId}")
	public ResponseEntity<Page<BookDTO>> getBooksByAuthor(@PathVariable Integer authorId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(bookService.getBooksByAuthor(authorId, pageable));
	}

	@GetMapping("/api/books/favoured")
	public ResponseEntity<Page<BookDTO>> getUserFavouredBooks(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		User user = userService.findUserByJwt(jwt);
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(bookService.getFollowedBooksByUserId(user.getId(), pageable));
	}

	@GetMapping("/categories/{categoryId}/books")
	public ResponseEntity<Page<BookDTO>> getBooksByCategory(@PathVariable Integer categoryId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(bookService.getBooksByCategoryId(categoryId, pageable));
	}

	@GetMapping("/books/search")
	public ResponseEntity<Page<BookDTO>> searchBooks(@RequestParam(required = false) String title,
			@RequestParam(required = false) Integer categoryId, @RequestParam(required = false) List<Integer> tagIds,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		return ResponseEntity.ok(bookService.searchBooks(title, categoryId, tagIds, pageable));
	}

	@GetMapping("/books/count")
	public ResponseEntity<?> getBookCount() {
		return ResponseEntity.ok(bookService.getBookCount());
	}

	@GetMapping("/books/{bookId}/comments-count")
	public Long getBookCommentCountCount(@PathVariable Integer bookId) {
		return bookService.getCommentCountForBook(bookId);
	}

	@GetMapping("/books/{bookId}")
	public ResponseEntity<BookDTO> getBookById(@PathVariable Integer bookId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		BookDTO bookDTO = bookService.getBookById(bookId);
		boolean isAuthenticated = jwt != null;
		if (isAuthenticated) {
			User user = userService.findUserByJwt(jwt);
			if (user != null) {
				bookDTO.setFollowedByCurrentUser(bookService.isBookLikedByUser(user.getId(), bookId));
			}
		}

		return ResponseEntity.ok(bookDTO);
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

	@GetMapping("/top-categories")
	public ResponseEntity<List<CategoryDTO>> getTopSixCategoriesWithBooks() {
		return ResponseEntity.ok(bookService.getTopSixCategoriesWithBooks());
	}

	@GetMapping("/books/latest-update")
	public ResponseEntity<List<BookDTO>> getLatestUpdateBooks(@RequestParam(defaultValue = "5") int limit) {
		return ResponseEntity.ok(bookService.getTopRecentChapterBooks(limit));
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
	public ResponseEntity<?> updateBook(@PathVariable("bookId") Integer bookId, @RequestBody BookDTO bookDTO,
			@RequestHeader("Authorization") String jwt) {
		BookDTO updatedBook = bookService.updateBook(bookId, bookDTO);
		User user = userService.findUserByJwt(jwt);
		Integer authorId = updatedBook.getAuthor().getId();
		if (user.getId() != authorId && user.getRole().getName().equals("ADMIN")) {
			return new ResponseEntity<>("You dont have any permission to edit this chapter", HttpStatus.UNAUTHORIZED);
		}
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + updatedBook.getTitle() + "' has been updated!");
		}
		return ResponseEntity.ok(updatedBook);
	}

	@DeleteMapping("/api/books/{bookId}")
	public ResponseEntity<?> deleteBook(@PathVariable("bookId") Integer bookId,
			@RequestHeader("Authorization") String jwt) {
		BookDTO book = bookService.getBookById(bookId);
		bookService.deleteBook(bookId);
		User user = userService.findUserByJwt(jwt);
		Integer authorId = book.getAuthor().getId();
		if (user.getId() != authorId && user.getRole().getName().equals("ADMIN")) {
			return new ResponseEntity<>("You dont have any permission to edit this chapter", HttpStatus.UNAUTHORIZED);
		}
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