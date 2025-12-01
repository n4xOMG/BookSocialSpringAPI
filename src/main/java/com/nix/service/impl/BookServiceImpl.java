package com.nix.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.nix.dtos.BookDTO;
import com.nix.dtos.BookPerformanceDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.dtos.mappers.CategoryMapper;
import com.nix.enums.NotificationEntityType;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.BookFavourite;
import com.nix.models.BookViewHistory;
import com.nix.models.Category;
import com.nix.models.User;
import com.nix.repository.BookFavouriteRepository;
import com.nix.repository.BookRepository;
import com.nix.repository.BookViewHistoryRepository;
import com.nix.repository.CategoryRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.TagRepository;
import com.nix.repository.UserRepository;
import com.nix.service.BookService;
import com.nix.service.ImageService;
import com.nix.service.NotificationService;

@Service
public class BookServiceImpl implements BookService {

	@Autowired
	BookRepository bookRepo;

	@Autowired
	BookFavouriteRepository bookFavouriteRepository;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	TagRepository tagRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NotificationService notificationService;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	BookViewHistoryRepository bookViewHistoryRepository;

	@Autowired
	ImageService imageService;

	@Autowired
	BookMapper bookMapper;

	@Autowired
	CategoryMapper categoryMapper;

	@Override
	public Page<BookDTO> getAllBooks(Pageable pageable) {
		Page<Book> booksPage = bookRepo.findAll(pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getAllBooks(Pageable pageable, Set<UUID> excludedAuthorIds) {
		if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
			return getAllBooks(pageable);
		}
		Page<Book> booksPage = bookRepo.findAllExcludingAuthors(excludedAuthorIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable) {
		Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
		if (!categoryOpt.isPresent()) {
			throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
		}
		Page<Book> booksPage = bookRepo.findByCategory(categoryOpt.get(), pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable, Set<UUID> excludedAuthorIds) {
		if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
			return getBooksByCategoryId(categoryId, pageable);
		}
		categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + categoryId));
		Page<Book> booksPage = bookRepo.findByCategoryIdExcludingAuthors(categoryId, excludedAuthorIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable) {
		Page<Book> booksPage = bookRepo.findByAuthorId(authorId, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable, Set<UUID> excludedAuthorIds) {
		if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
			return getBooksByAuthor(authorId, pageable);
		}
		Page<Book> booksPage = bookRepo.findByAuthorIdExcludingAuthors(authorId, excludedAuthorIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable) {
		Page<Book> booksPage = bookRepo.searchBooks(title, categoryId, tagIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable,
			Set<UUID> excludedAuthorIds) {
		if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
			return searchBooks(title, categoryId, tagIds, pageable);
		}
		Page<Book> booksPage = bookRepo.searchBooksExcludingAuthors(title, categoryId, tagIds, excludedAuthorIds,
				pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> searchBooksForAuthor(UUID authorId, String query, Pageable pageable) {
		userRepository.findById(authorId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + authorId));

		Page<Book> booksPage;
		if (!StringUtils.hasText(query)) {
			booksPage = bookRepo.findByAuthorId(authorId, pageable);
		} else {
			booksPage = bookRepo.findByAuthorIdAndTitleContainingIgnoreCase(authorId, query.trim(), pageable);
		}

		return booksPage.map(bookMapper::mapToDTO);
	}

	@Override
	public Page<BookDTO> getFollowedBooksByUserId(UUID userId, Pageable pageable) {
		userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		Page<BookFavourite> favouritesPage = bookFavouriteRepository.findByUserId(userId, pageable);
		List<BookDTO> favouriteBooks = favouritesPage.getContent().stream()
				.map(BookFavourite::getBook)
				.map(bookMapper::mapToDTO)
				.collect(Collectors.toList());

		return new PageImpl<>(favouriteBooks, pageable, favouritesPage.getTotalElements());
	}

	@Override
	public Page<BookDTO> getFollowedBooksByUserId(UUID userId, Pageable pageable, Set<UUID> excludedAuthorIds) {
		if (excludedAuthorIds == null || excludedAuthorIds.isEmpty()) {
			return getFollowedBooksByUserId(userId, pageable);
		}
		userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		Page<Book> booksPage = bookRepo.findFollowedBooksByUserIdExcludingAuthors(userId, excludedAuthorIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Long getBookCount() {
		return bookRepo.count();
	}

	@Override
	public BookDTO getBookById(UUID bookId) {
		Book book = getBookOrThrow(bookId);
		return bookMapper.mapToDTO(book);
	}

	@Override
	@Transactional
	public BookDTO createBook(BookDTO bookDTO, UUID authorId) throws IOException {
		if (authorId == null) {
			throw new IllegalArgumentException("Author identifier is required to create a book.");
		}
		if (bookDTO == null) {
			throw new IllegalArgumentException("Book payload must not be null.");
		}

		User author = userRepository.findById(authorId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + authorId));
		if (bookDTO.getCategoryId() == null) {
			throw new IllegalArgumentException("Category identifier is required to create a book.");
		}

		Book book = new Book();
		book.setAuthor(author);
		book.setTitle(bookDTO.getTitle());
		book.setAuthorName(bookDTO.getAuthorName());
		book.setArtistName(bookDTO.getArtistName());
		book.setDescription(bookDTO.getDescription());
		if (bookDTO.getBookCover() != null) {
			book.setBookCover(new com.nix.models.ImageAttachment(bookDTO.getBookCover().getUrl(),
					bookDTO.getBookCover().getIsMild()));
		}
		book.setLanguage(bookDTO.getLanguage());
		book.setStatus(bookDTO.getStatus());
		book.setUploadDate(LocalDateTime.now());
		book.setSuggested(false);

		book.setCategory(categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId())));

		List<Integer> tagIds = bookDTO.getTagIds() != null ? bookDTO.getTagIds() : Collections.emptyList();
		book.setTags(tagRepository.findAllById(tagIds));

		return bookMapper.mapToDTO(bookRepo.save(book));
	}

	@Override
	@Transactional
	public BookDTO updateBook(UUID bookId, BookDTO bookDTO) {
		Book existingBook = getBookOrThrow(bookId);
		existingBook.setTitle(bookDTO.getTitle());
		if (bookDTO.getBookCover() != null) {
			existingBook.setBookCover(new com.nix.models.ImageAttachment(bookDTO.getBookCover().getUrl(),
					bookDTO.getBookCover().getIsMild()));
		}
		existingBook.setAuthorName(bookDTO.getAuthorName());
		existingBook.setArtistName(bookDTO.getArtistName());
		existingBook.setDescription(bookDTO.getDescription());
		existingBook.setLanguage(bookDTO.getLanguage());
		existingBook.setStatus(bookDTO.getStatus());
		existingBook.setSuggested(bookDTO.isSuggested());
		existingBook.setCategory(categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId())));
		List<Integer> tagIds = bookDTO.getTagIds() != null ? bookDTO.getTagIds() : Collections.emptyList();
		existingBook.setTags(tagRepository.findAllById(tagIds));
		return bookMapper.mapToDTO(bookRepo.save(existingBook));
	}

	@Override
	@Transactional
	public void deleteBook(UUID bookId) {
		Book existingBook = getBookOrThrow(bookId);

		// Delete all book view history records for this book
		bookViewHistoryRepository.deleteByBook(existingBook);

		// Remove this book from tags
		existingBook.getTags().clear();

		bookRepo.delete(existingBook);
	}

	@Override
	public List<BookDTO> getTop10LikedBooks() {
		return bookMapper.mapToDTOs(bookRepo.findTopBooksByLikes(PageRequest.of(0, 10)));
	}

	@Override
	public List<BookDTO> getFeaturedBooks() {
		return bookMapper.mapToDTOs(bookRepo.findByIsSuggested(true));
	}

	@Override
	public List<BookDTO> getRelatedBooks(UUID bookId, List<Integer> tagIds) {
		Book currentBook = getBookOrThrow(bookId);
		return bookMapper.mapToDTOs(
				bookRepo.findRelatedBooks(currentBook.getCategory().getId(), tagIds, bookId, PageRequest.of(0, 5)));
	}

	@Override
	public List<BookDTO> getBooksBySuggestedStatus(Boolean isSuggested) {
		return bookMapper.mapToDTOs(bookRepo.findByIsSuggested(isSuggested));
	}

	@Override
	public List<BookDTO> getBooksByStatus(String status) {
		return bookMapper.mapToDTOs(bookRepo.findByStatus(status));
	}

	@Override
	public List<CategoryDTO> getTopSixCategoriesWithBooks() {
		List<Category> categories = categoryRepository.findTop6ByOrderByNameAsc();
		if (categories.isEmpty()) {
			return Collections.emptyList();
		}
		return categoryMapper.mapToDTOs(categories);
	}

	@Override
	@Transactional
	public boolean markAsFavouriteBook(UUID bookId, User user) {
		Book book = getBookOrThrow(bookId);
		Optional<BookFavourite> existingFavourite = bookFavouriteRepository.findByBookAndUser(book, user);

		if (existingFavourite.isPresent()) {
			bookFavouriteRepository.delete(existingFavourite.get());
			return false;
		}

		BookFavourite favourite = new BookFavourite();
		favourite.setBook(book);
		favourite.setUser(user);
		favourite.setFavoritedDate(LocalDateTime.now());
		bookFavouriteRepository.save(favourite);

		String message = "User " + user.getUsername() + " favoured your book!";
		notificationService.createNotification(book.getAuthor(), message, NotificationEntityType.BOOK, book.getId());
		return true;
	}

	@Override
	@Transactional
	public long recordBookView(UUID bookId, UUID viewerId, String viewerIp) {
		Book book = getBookOrThrow(bookId);
		book.setViewCount(book.getViewCount() + 1);
		bookRepo.save(book);
		updateDailyViewStats(book);
		return book.getViewCount();
	}

	@Override
	public Set<UUID> getFavouriteBookIdsForUser(UUID userId) {
		userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		return new HashSet<>(bookFavouriteRepository.findBookIdsByUserId(userId));
	}

	@Override
	public BookDTO setEditorChoice(UUID bookId, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
		existingBook.setSuggested(!existingBook.isSuggested());
		String message = "Your book has been set as editor choice!";
		notificationService.createNotification(existingBook.getAuthor(), message, NotificationEntityType.BOOK, bookId);

		return bookMapper.mapToDTO(bookRepo.save(existingBook));
	}

	@Override
	public boolean isBookLikedByUser(UUID userId, UUID bookId) {
		userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		getBookOrThrow(bookId);
		return bookFavouriteRepository.existsByBookIdAndUserId(bookId, userId);
	}

	@Override
	public List<BookDTO> getTopRecentChapterBooks(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return bookMapper.mapToDTOs(bookRepo.findTopBooksWithLatestChapters(pageRequest));
	}

	@Override
	public Long getCommentCountForBook(UUID bookId) {
		return commentRepository.countCommentsByBookId(bookId);
	}

	@Override
	public List<Long> getBookUploadedPerMonthNumber() {
		return bookRepo.countBooksUploadedPerMonth();
	}

	@Override
	public List<BookDTO> getTrendingBooks(int hours, long minViews, int limit) {
		LocalDateTime since = LocalDateTime.now().minusHours(hours);
		PageRequest pageRequest = PageRequest.of(0, limit);
		List<Book> trendingBooks = bookViewHistoryRepository.findTrendingBooks(since, minViews, pageRequest);
		return bookMapper.mapToDTOs(trendingBooks);
	}

	@Override
	public List<BookPerformanceDTO> getAuthorBookPerformance(UUID authorId) {
		Page<Book> authorBooks = bookRepo.findByAuthorId(authorId, Pageable.unpaged());
		List<BookPerformanceDTO> performanceList = new ArrayList<>();

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime oneDayAgo = now.minusDays(1);
		LocalDateTime oneWeekAgo = now.minusWeeks(1);
		LocalDateTime oneMonthAgo = now.minusMonths(1);

		for (Book book : authorBooks.getContent()) {
			BookPerformanceDTO performance = new BookPerformanceDTO();
			performance.setBookId(book.getId());
			performance.setTitle(book.getTitle());
			if (book.getBookCover() != null) {
				performance.setBookCover(new com.nix.dtos.ImageAttachmentDTO(book.getBookCover().getUrl(),
						book.getBookCover().getIsMild()));
			}
			performance.setStatus(book.getStatus());
			performance.setTotalChapters(book.getChapters().size());
			performance.setLastUpdated(book.getUploadDate());

			// Current metrics
			performance.setCurrentViews(book.getViewCount());
			performance.setCurrentFavourites(Math.toIntExact(bookFavouriteRepository.countByBookId(book.getId())));
			performance.setCurrentComments(commentRepository.countCommentsByBookId(book.getId()));

			// Calculate growth metrics using view history
			long dailyViews = bookViewHistoryRepository.getViewCountForBookBetweenDates(
					book.getId(), oneDayAgo, now);
			long weeklyViews = bookViewHistoryRepository.getViewCountForBookBetweenDates(
					book.getId(), oneWeekAgo, now);
			long monthlyViews = bookViewHistoryRepository.getViewCountForBookBetweenDates(
					book.getId(), oneMonthAgo, now);

			performance.setDailyViewsGrowth(dailyViews);
			performance.setWeeklyViewsGrowth(weeklyViews);
			performance.setMonthlyViewsGrowth(monthlyViews);

			// For favourites and comments, we'll show current numbers as growth
			// (since we don't track historical data for these yet)
			performance.setDailyFavouritesGrowth(0); // Could be enhanced later
			performance.setWeeklyFavouritesGrowth(0);
			performance.setMonthlyFavouritesGrowth(0);

			performance.setDailyCommentsGrowth(0); // Could be enhanced later
			performance.setWeeklyCommentsGrowth(0);
			performance.setMonthlyCommentsGrowth(0);

			performanceList.add(performance);
		}

		return performanceList;
	}

	private void updateDailyViewStats(Book book) {
		LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

		Optional<BookViewHistory> existingHistory = bookViewHistoryRepository.findByBookAndDate(book, today);

		if (existingHistory.isPresent()) {
			BookViewHistory history = existingHistory.get();
			history.setDailyViewCount(history.getDailyViewCount() + 1);
			bookViewHistoryRepository.save(history);
		} else {
			BookViewHistory newHistory = new BookViewHistory(book, today, 1);
			bookViewHistoryRepository.save(newHistory);
		}
	}

	private Book getBookOrThrow(UUID bookId) {
		return bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
	}

}
