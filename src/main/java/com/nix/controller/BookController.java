package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
import com.nix.dtos.mappers.BookMapper;
import com.nix.dtos.mappers.CategoryMapper;
import com.nix.dtos.mappers.RatingMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.User;
import com.nix.service.BookService;
import com.nix.service.CategoryService;
import com.nix.service.RatingService;
import com.nix.service.UserService;

@RestController
public class BookController {

	@Autowired
	BookService bookService;

	@Autowired
	UserService userService;

	@Autowired
	RatingService ratingService;

	@Autowired
	CategoryService categoryService;

	BookMapper bookMapper = new BookMapper();

	RatingMapper ratingMapper = new RatingMapper();

	CategoryMapper categoryMapper = new CategoryMapper();

	@Cacheable("books")
	@GetMapping("/books")
	public ResponseEntity<List<BookDTO>> getAllBooks() {
		List<Book> books = bookService.getAllBooks();
		return ResponseEntity.ok(bookMapper.mapToDTOs(books));
	}

	@GetMapping("/books/{bookId}")
	public ResponseEntity<BookDTO> getBookById(@PathVariable Integer bookId) {
		Book book = bookService.getBookById(bookId);
		return ResponseEntity.ok(bookMapper.mapToDTO(book));
	}

	@GetMapping("/books/top-likes")
	public ResponseEntity<List<BookDTO>> getTop10BooksByLikes() {
		List<Book> books = bookService.getTop10LikedBooks();
		return ResponseEntity.ok(bookMapper.mapToDTOs(books));
	}

	@GetMapping("/books/featured")
	public ResponseEntity<?> getFeaturedBooks() {
		try {
			List<Book> featuredBooks = bookService.getFeaturedBooks();
			return ResponseEntity.ok(bookMapper.mapToDTOs(featuredBooks));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching featured books.");
		}
	}

	@GetMapping("/books/{bookId}/related")
	public ResponseEntity<?> getRelatedBooks(@PathVariable("bookId") Integer bookId,
			@RequestParam(value = "tagIds", required = false) List<Integer> tagIds) {
		try {
			List<Book> relatedBooks = bookService.getRelatedBooks(bookId, tagIds);
			return new ResponseEntity<>(bookMapper.mapToDTOs(relatedBooks), HttpStatus.OK);
		} catch (IllegalArgumentException | ResourceNotFoundException ex) {
			return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return new ResponseEntity<>("Failed to fetch related books.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/books/search")
	public ResponseEntity<List<BookDTO>> searchBooks(@RequestParam(required = false) String title,
			@RequestParam(required = false) Integer categoryId, @RequestParam(required = false) List<Integer> tagIds) {
		List<Book> books = bookService.searchBooks(title, categoryId, tagIds);
		return ResponseEntity.ok(bookMapper.mapToDTOs(books));
	}

	@GetMapping("/books/author/{authorId}")
	public ResponseEntity<List<BookDTO>> getBooksByAuthor(@PathVariable Integer authorId) {
		List<Book> books = bookService.getBooksByAuthor(authorId);
		return ResponseEntity.ok(bookMapper.mapToDTOs(books));
	}

	@GetMapping("/top-categories")
	public ResponseEntity<?> getTopSixCategoriesWithBooks() {
		try {
			List<Category> categories = bookService.getTopSixCategoriesWithBooks();
			return ResponseEntity.ok(categoryMapper.mapToDTOs(categories));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching categories.");
		}
	}

	@GetMapping("/api/books/favoured")
	public ResponseEntity<List<BookDTO>> getUserFavouredBooks(@RequestHeader("Authorization") String jwt)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (user == null) {
			throw new Exception("No user found");
		}
		List<BookDTO> books = bookMapper.mapToDTOs(bookService.getFollowedBooksByUserId(user.getId()));

		return new ResponseEntity<List<BookDTO>>(books, HttpStatus.ACCEPTED);
	}

	@GetMapping("/books/latest-update")
	public ResponseEntity<?> getLatestUpdateBooks(@RequestParam(defaultValue = "5") int limit) {
		List<Book> recentBooks = bookService.getTopRecentChapterBooks(limit);
		return ResponseEntity.ok(bookMapper.mapToDTOs(recentBooks));
	}

	@GetMapping("/categories/{categoryId}/books")
	public ResponseEntity<?> getBooksByCategory(@PathVariable Integer categoryId) {
		try {
			List<Book> books = bookService.getBooksByCategoryId(categoryId);
			return ResponseEntity.ok(bookMapper.mapToDTOs(books));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	/**
	 * Endpoint to check if a book is liked by the user.
	 */
	@GetMapping("/api/books/{bookId}/isLiked")
	public ResponseEntity<Boolean> checkBookLikedByUser(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer bookId) {
		try {
			User user = userService.findUserByJwt(jwt);
			boolean isLiked = bookService.isBookLikedByUser(user.getId(), bookId);
			return ResponseEntity.ok(isLiked);
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
		}
	}

	@PostMapping("/api/books")
	public ResponseEntity<BookDTO> createBook(@RequestBody BookDTO bookDTO) {
		Book created = bookService.createBook(bookDTO);
		return ResponseEntity.ok(bookMapper.mapToDTO(created));
	}

	@PutMapping("/api/books/{bookId}")
	public ResponseEntity<BookDTO> updateBook(@PathVariable("bookId") Integer bookId, @RequestBody BookDTO book) {
		Book updated = bookService.updateBook(bookId, book);
		return ResponseEntity.ok(bookMapper.mapToDTO(updated));
	}

	@DeleteMapping("/api/books/{bookId}")
	public ResponseEntity<Void> deleteBook(@PathVariable("bookId") Integer bookId) {
		bookService.deleteBook(bookId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/api/books/follow/{bookId}")
	public ResponseEntity<Boolean> markBookAsFavoured(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer bookId) {
		User reqUser = userService.findUserByJwt(jwt);
		if (reqUser != null) {
			Boolean isFollowed = bookService.markAsFavouriteBook(bookService.getBookById(bookId), reqUser);
			return new ResponseEntity<>(isFollowed, HttpStatus.ACCEPTED);
		}

		return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
	}

	@PutMapping("/api/books/{bookId}/editor-choice")
	public ResponseEntity<BookDTO> setEditorChoice(@RequestBody BookDTO book, @PathVariable Integer bookId) {
		BookDTO editorChoiceBook = bookMapper.mapToDTO(bookService.setEditorChoice(bookId, book));
		return new ResponseEntity<>(editorChoiceBook, HttpStatus.ACCEPTED);
	}

}
