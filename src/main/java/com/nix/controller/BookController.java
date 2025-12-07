package com.nix.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import com.nix.enums.NotificationEntityType;
import com.nix.exception.ForbiddenAccessException;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
import com.nix.service.BookService;
import com.nix.service.NotificationService;
import com.nix.service.UserService;
import com.nix.util.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class BookController {

	@Autowired
	private BookService bookService;

	@Autowired
	private UserService userService;

	@Autowired
	private NotificationService notificationService;

	private User resolveCurrentUser(String jwt) {
		if (jwt == null || jwt.isBlank()) {
			return null;
		}
		return userService.findUserByJwt(jwt);
	}

	private Set<UUID> getHiddenAuthorIds(User currentUser) {
		if (currentUser == null) {
			return Set.of();
		}
		if (SecurityUtils.isAdmin(currentUser)) {
			return Set.of();
		}
		Set<UUID> hiddenAuthorIds = new HashSet<>(userService.getUserIdsBlocking(currentUser.getId()));
		Set<UUID> blockedUserIds = userService.getBlockedUserIds(currentUser.getId());
		hiddenAuthorIds.addAll(blockedUserIds);
		return hiddenAuthorIds;
	}

	private List<BookDTO> filterBooks(List<BookDTO> books, Set<UUID> hiddenAuthorIds) {
		if (hiddenAuthorIds == null || hiddenAuthorIds.isEmpty()) {
			return books;
		}
		if (books == null) {
			return Collections.emptyList();
		}
		return books.stream()
				.filter(book -> book.getAuthor() == null || !hiddenAuthorIds.contains(book.getAuthor().getId()))
				.collect(Collectors.toList());
	}

	private boolean isInteractionBlocked(User currentUser, UUID ownerId) {
		if (currentUser == null || ownerId == null) {
			return false;
		}
		if (SecurityUtils.isAdmin(currentUser)) {
			return false;
		}
		return userService.isBlockedBy(currentUser.getId(), ownerId)
				|| userService.hasBlocked(currentUser.getId(), ownerId);
	}

	private void ensureNotBlocked(User currentUser, UUID ownerId) {
		if (isInteractionBlocked(currentUser, ownerId)) {
			throw new ForbiddenAccessException(
					"You cannot access this resource because one of the accounts has blocked the other.");
		}
	}

	@GetMapping("/books")
	public ResponseEntity<Page<BookDTO>> getAllBooks(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		User currentUser = resolveCurrentUser(jwt);

		// Use database-level filtering for better performance
		Page<BookDTO> booksPage;
		if (currentUser != null) {
			Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(currentUser);
			booksPage = bookService.getAllBooks(pageable, hiddenAuthorIds);
		} else {
			booksPage = bookService.getAllBooks(pageable);
		}

		if (currentUser != null) {
			Set<UUID> favouriteBookIds = bookService.getFavouriteBookIdsForUser(currentUser.getId());
			booksPage.getContent().forEach(bookDTO -> bookDTO
					.setFollowedByCurrentUser(favouriteBookIds.contains(bookDTO.getId())));
		}

		return ResponseEntity.ok(booksPage);
	}

	@GetMapping("/books/books-upload-per-month")
	public ResponseEntity<ApiResponseWithData<List<Long>>> getBooksUploadedPerMonthCount() {
		List<Long> counts = bookService.getBookUploadedPerMonthNumber();
		return ResponseEntity
				.ok(new ApiResponseWithData<>("Monthly upload counts retrieved successfully.", true, counts));
	}

	@GetMapping("/books/author/{authorId}")
	public ResponseEntity<Page<BookDTO>> getBooksByAuthor(@PathVariable UUID authorId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		User currentUser = resolveCurrentUser(jwt);
		if (currentUser != null) {
			ensureNotBlocked(currentUser, authorId);
		}

		// Use database-level filtering
		Page<BookDTO> booksPage;
		if (currentUser != null) {
			Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(currentUser);
			booksPage = bookService.getBooksByAuthor(authorId, pageable, hiddenAuthorIds);
		} else {
			booksPage = bookService.getBooksByAuthor(authorId, pageable);
		}
		return ResponseEntity.ok(booksPage);
	}

	@GetMapping("/api/books/favoured")
	public ResponseEntity<Page<BookDTO>> getUserFavouredBooks(@RequestHeader("Authorization") String jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy) {
		User user = userService.findUserByJwt(jwt);
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(user);
		Page<BookDTO> favourites = bookService.getFollowedBooksByUserId(user.getId(), pageable, hiddenAuthorIds);
		return ResponseEntity.ok(favourites);
	}

	@GetMapping("/categories/{categoryId}/books")
	public ResponseEntity<Page<BookDTO>> getBooksByCategory(@PathVariable Integer categoryId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		User currentUser = resolveCurrentUser(jwt);

		// Use database-level filtering
		Page<BookDTO> booksPage;
		if (currentUser != null) {
			Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(currentUser);
			booksPage = bookService.getBooksByCategoryId(categoryId, pageable, hiddenAuthorIds);
		} else {
			booksPage = bookService.getBooksByCategoryId(categoryId, pageable);
		}
		return ResponseEntity.ok(booksPage);
	}

	@GetMapping("/books/search")
	public ResponseEntity<Page<BookDTO>> searchBooks(@RequestParam(required = false) String title,
			@RequestParam(required = false) Integer categoryId, @RequestParam(required = false) List<Integer> tagIds,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
		User currentUser = resolveCurrentUser(jwt);

		// Use database-level filtering
		Page<BookDTO> booksPage;
		if (currentUser != null) {
			Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(currentUser);
			booksPage = bookService.searchBooks(title, categoryId, tagIds, pageable, hiddenAuthorIds);
		} else {
			booksPage = bookService.searchBooks(title, categoryId, tagIds, pageable);
		}
		return ResponseEntity.ok(booksPage);
	}

	@GetMapping("/books/count")
	public ResponseEntity<ApiResponseWithData<Long>> getBookCount() {
		Long count = bookService.getBookCount();
		return ResponseEntity.ok(new ApiResponseWithData<>("Book count retrieved successfully.", true, count));
	}

	@GetMapping("/books/{bookId}/comments-count")
	public ResponseEntity<ApiResponseWithData<Long>> getBookCommentCountCount(@PathVariable UUID bookId) {
		Long count = bookService.getCommentCountForBook(bookId);
		return ResponseEntity.ok(new ApiResponseWithData<>("Comment count retrieved successfully.", true, count));
	}

	@GetMapping("/books/{bookId}")
	public ResponseEntity<BookDTO> getBookById(@PathVariable UUID bookId,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		BookDTO bookDTO = bookService.getBookById(bookId);
		User currentUser = resolveCurrentUser(jwt);
		if (currentUser != null) {
			UUID ownerId = bookDTO.getAuthor() != null ? bookDTO.getAuthor().getId() : null;
			ensureNotBlocked(currentUser, ownerId);
			bookDTO.setFollowedByCurrentUser(bookService.isBookLikedByUser(currentUser.getId(), bookId));
		}

		return ResponseEntity.ok(bookDTO);
	}

	@GetMapping("/books/top-likes")
	public ResponseEntity<List<BookDTO>> getTop10BooksByLikes(
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		List<BookDTO> books = bookService.getTop10LikedBooks();
		if (currentUser != null) {
			books = filterBooks(books, getHiddenAuthorIds(currentUser));
		}
		return ResponseEntity.ok(books);
	}

	@GetMapping("/books/featured")
	public ResponseEntity<List<BookDTO>> getFeaturedBooks(
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		List<BookDTO> books = bookService.getFeaturedBooks();
		if (currentUser != null) {
			books = filterBooks(books, getHiddenAuthorIds(currentUser));
		}
		return ResponseEntity.ok(books);
	}

	@GetMapping("/books/{bookId}/related")
	public ResponseEntity<List<BookDTO>> getRelatedBooks(@PathVariable("bookId") UUID bookId,
			@RequestParam(value = "tagIds", required = false) List<Integer> tagIds,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		if (currentUser != null) {
			BookDTO baseBook = bookService.getBookById(bookId);
			UUID ownerId = baseBook.getAuthor() != null ? baseBook.getAuthor().getId() : null;
			ensureNotBlocked(currentUser, ownerId);
		}
		List<BookDTO> related = bookService.getRelatedBooks(bookId, tagIds);
		if (currentUser != null) {
			related = filterBooks(related, getHiddenAuthorIds(currentUser));
		}
		return ResponseEntity.ok(related);
	}

	@GetMapping("/top-categories")
	public ResponseEntity<List<CategoryDTO>> getTopSixCategoriesWithBooks(
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		List<CategoryDTO> categories = bookService.getTopSixCategoriesWithBooks();
		if (currentUser != null) {
			Set<UUID> hiddenAuthorIds = getHiddenAuthorIds(currentUser);
			if (!hiddenAuthorIds.isEmpty()) {
				categories.forEach(category -> {
					List<BookDTO> filteredBooks = filterBooks(category.getBooks(), hiddenAuthorIds);
					category.setBooks(filteredBooks);
				});
			}
		}
		return ResponseEntity.ok(categories);
	}

	@GetMapping("/books/latest-update")
	public ResponseEntity<List<BookDTO>> getLatestUpdateBooks(@RequestParam(defaultValue = "5") int limit,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		List<BookDTO> books = bookService.getTopRecentChapterBooks(limit);
		if (currentUser != null) {
			books = filterBooks(books, getHiddenAuthorIds(currentUser));
		}
		return ResponseEntity.ok(books);
	}

	@GetMapping("/books/trending")
	public ResponseEntity<List<BookDTO>> getTrendingBooks(
			@RequestParam(defaultValue = "24") int hours,
			@RequestParam(defaultValue = "10") long minViews,
			@RequestParam(defaultValue = "10") int limit,
			@RequestHeader(value = "Authorization", required = false) String jwt) {
		User currentUser = resolveCurrentUser(jwt);
		List<BookDTO> books = bookService.getTrendingBooks(hours, minViews, limit);
		if (currentUser != null) {
			books = filterBooks(books, getHiddenAuthorIds(currentUser));
		}
		return ResponseEntity.ok(books);
	}

	@GetMapping("/api/books/{bookId}/isLiked")
	public ResponseEntity<ApiResponseWithData<Boolean>> checkBookLikedByUser(@RequestHeader("Authorization") String jwt,
			@PathVariable UUID bookId) {
		User user = userService.findUserByJwt(jwt);
		Boolean isLiked = bookService.isBookLikedByUser(user.getId(), bookId);
		return ResponseEntity.ok(new ApiResponseWithData<>("Like status retrieved successfully.", true, isLiked));
	}

	@PostMapping("/api/books")
	public ResponseEntity<ApiResponseWithData<BookDTO>> createBook(@RequestBody BookDTO bookDTO,
			@RequestHeader("Authorization") String jwt) throws IOException {
		User user = userService.findUserByJwt(jwt);
		if (user.isBanned()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"You are currently banned from this website. Contact support for assistance.", false));
		}
		if (user.getIsSuspended()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"You are currently suspended. Contact support for assistance.", false));
		}
		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"Please verify your account before creating books.", false));
		}
		BookDTO createdBook = bookService.createBook(bookDTO, user.getId());
		UUID authorId = createdBook.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + createdBook.getTitle() + "' has been created!", NotificationEntityType.BOOK,
					createdBook.getId());
		}
		ApiResponseWithData<BookDTO> response = new ApiResponseWithData<>("Book created successfully.", true,
				createdBook);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/api/books/{bookId}")
	public ResponseEntity<ApiResponseWithData<BookDTO>> updateBook(@PathVariable("bookId") UUID bookId,
			@RequestBody BookDTO bookDTO, @RequestHeader("Authorization") String jwt) {
		BookDTO book = bookService.getBookById(bookId);
		User user = userService.findUserByJwt(jwt);
		UUID authorId = book.getAuthor() != null ? book.getAuthor().getId() : null;
		boolean isAdmin = SecurityUtils.isAdmin(user);

		if (!isAdmin && (authorId == null || !user.getId().equals(authorId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"You do not have permission to edit this book.", false));
		}
		BookDTO updatedBook = bookService.updateBook(bookId, bookDTO);
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + updatedBook.getTitle() + "' has been updated!", NotificationEntityType.BOOK,
					bookId);
		}
		ApiResponseWithData<BookDTO> response = new ApiResponseWithData<>("Book updated successfully.", true,
				updatedBook);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/api/books/{bookId}")
	public ResponseEntity<ApiResponseWithData<Void>> deleteBook(@PathVariable("bookId") UUID bookId,
			@RequestHeader("Authorization") String jwt) {
		BookDTO book = bookService.getBookById(bookId);
		User user = userService.findUserByJwt(jwt);

		UUID authorId = book.getAuthor() != null ? book.getAuthor().getId() : null;
		boolean isAdmin = SecurityUtils.isAdmin(user);
		if (!isAdmin && (authorId == null || !user.getId().equals(authorId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponseWithData<>(
					"You do not have permission to delete this book.", false));
		}
		bookService.deleteBook(bookId);
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author, "Your book '" + book.getTitle() + "' has been deleted!",
					NotificationEntityType.BOOK, null);
		}
		ApiResponseWithData<Void> response = new ApiResponseWithData<>("Book deleted successfully.", true);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/books/{bookId}/views")
	public ResponseEntity<ApiResponseWithData<Long>> recordBookView(@PathVariable UUID bookId,
			@RequestHeader(value = "Authorization", required = false) String jwt,
			HttpServletRequest request) {
		UUID viewerId = null;
		if (jwt != null && !jwt.isBlank()) {
			User viewer = userService.findUserByJwt(jwt);
			if (viewer != null) {
				BookDTO bookDTO = bookService.getBookById(bookId);
				UUID ownerId = bookDTO.getAuthor() != null ? bookDTO.getAuthor().getId() : null;
				if (isInteractionBlocked(viewer, ownerId)) {
					ApiResponseWithData<Long> blockedResponse = new ApiResponseWithData<>(
							"You cannot record a view for this book because access between the accounts is blocked.",
							false);
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blockedResponse);
				}
				viewerId = viewer.getId();
			}
		}
		long updatedCount = bookService.recordBookView(bookId, viewerId, request.getRemoteAddr());
		ApiResponseWithData<Long> successResponse = new ApiResponseWithData<>(
				"Book view recorded successfully.", true, updatedCount);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(successResponse);
	}

	@PutMapping("/api/books/follow/{bookId}")
	public ResponseEntity<ApiResponseWithData<Boolean>> markBookAsFavoured(
			@RequestHeader("Authorization") String jwt, @PathVariable UUID bookId) {
		User reqUser = userService.findUserByJwt(jwt);
		BookDTO bookDTO = bookService.getBookById(bookId);
		UUID ownerId = bookDTO.getAuthor() != null ? bookDTO.getAuthor().getId() : null;
		if (isInteractionBlocked(reqUser, ownerId)) {
			ApiResponseWithData<Boolean> blockedResponse = new ApiResponseWithData<>(
					"You cannot follow or unfollow this book because access between the accounts is blocked.", false);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blockedResponse);
		}
		boolean isFollowed = bookService.markAsFavouriteBook(bookId, reqUser);
		String message = isFollowed ? "Book added to favourites." : "Book removed from favourites.";
		ApiResponseWithData<Boolean> successResponse = new ApiResponseWithData<>(message, true, isFollowed);
		return ResponseEntity.ok(successResponse);
	}

	@PutMapping("/admin/books/{bookId}/editor-choice")
	public ResponseEntity<ApiResponseWithData<BookDTO>> setEditorChoice(@PathVariable UUID bookId,
			@RequestBody BookDTO bookDTO) {
		BookDTO updatedBook = bookService.setEditorChoice(bookId, bookDTO);
		UUID authorId = updatedBook.getAuthor().getId();
		if (authorId != null) {
			User author = userService.findUserById(authorId);
			notificationService.createNotification(author,
					"Your book '" + updatedBook.getTitle() + "' has been selected as Editor's Choice!",
					NotificationEntityType.BOOK, bookId);
		}
		return ResponseEntity
				.ok(new ApiResponseWithData<>("Book set as editor's choice successfully.", true, updatedBook));
	}
}