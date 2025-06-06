package com.nix.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.dtos.mappers.CategoryMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.UploadType;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.CategoryRepository;
import com.nix.repository.CommentRepository;
import com.nix.repository.TagRepository;
import com.nix.repository.UserRepository;

@Service
public class BookServiceImpl implements BookService {

	@Autowired
	BookRepository bookRepo;

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
	public Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable) {
		Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
		if (!categoryOpt.isPresent()) {
			throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
		}
		Page<Book> booksPage = bookRepo.findByCategory(categoryOpt.get(), pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getBooksByAuthor(Long authorId, Pageable pageable) {
		Page<Book> booksPage = bookRepo.findByAuthorId(authorId, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable) {
		Page<Book> booksPage = bookRepo.searchBooks(title, categoryId, tagIds, pageable);
		return booksPage.map(book -> bookMapper.mapToDTO(book));
	}

	@Override
	public Page<BookDTO> getFollowedBooksByUserId(Long userId, Pageable pageable) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		return bookRepo.findByUserFavoured(userId, pageable).map(book -> bookMapper.mapToDTO(book));

	}

	@Override
	public Long getBookCount() {
		return bookRepo.count();
	}

	@Override
	public BookDTO getBookById(Long bookId) {
		Book book = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
		book.setViewCount(book.getViewCount() + 1);
		return bookMapper.mapToDTO(book);
	}

	@Override
	@Transactional
	public BookDTO createBook(BookDTO bookDTO) throws IOException {
		Book book = new Book();
		book.setAuthor(userRepository.findById(bookDTO.getAuthor().getId()).orElseThrow(
				() -> new ResourceNotFoundException("User not found with ID: " + bookDTO.getAuthor().getId())));
		book.setTitle(bookDTO.getTitle());
		book.setAuthorName(bookDTO.getAuthorName());
		book.setArtistName(bookDTO.getArtistName());
		book.setDescription(bookDTO.getDescription());
		book.setBookCover(bookDTO.getBookCover());
		book.setLanguage(bookDTO.getLanguage());
		book.setStatus(bookDTO.getStatus());
		book.setUploadDate(LocalDateTime.now());
		book.setSuggested(false);

		book.setCategory(categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId())));

		book.setTags(tagRepository.findAllById(bookDTO.getTagIds()));

		return bookMapper.mapToDTO(bookRepo.save(book));
	}


	@Override
	@Transactional
	public BookDTO updateBook(Long bookId, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
		existingBook.setTitle(bookDTO.getTitle());
		existingBook.setBookCover(bookDTO.getBookCover());
		existingBook.setAuthorName(bookDTO.getAuthorName());
		existingBook.setArtistName(bookDTO.getArtistName());
		existingBook.setDescription(bookDTO.getDescription());
		existingBook.setLanguage(bookDTO.getLanguage());
		existingBook.setStatus(bookDTO.getStatus());
		existingBook.setSuggested(bookDTO.isSuggested());
		existingBook.setCategory(categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId())));
		existingBook.setTags(tagRepository.findAllById(bookDTO.getTagIds()));
		return bookMapper.mapToDTO(bookRepo.save(existingBook));
	}

	@Override
	@Transactional
	public void deleteBook(Long bookId) {
		Book existingBook = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

		// Remove this book from tags
		existingBook.getTags().clear();

		bookRepo.delete(existingBook);
	}

	@Override
	public List<BookDTO> getTop10LikedBooks() {
		return bookMapper.mapToDTOs(bookRepo.findTopBooksByLikes());
	}

	@Override
	public List<BookDTO> getFeaturedBooks() {
		return bookMapper.mapToDTOs(bookRepo.findByIsSuggested(true));
	}

	@Override
	public List<BookDTO> getRelatedBooks(Long bookId, List<Integer> tagIds) {
		Book currentBook = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
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
	public boolean markAsFavouriteBook(BookDTO bookDTO, User user) {
		Book book = bookRepo.findById(bookDTO.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookDTO.getId()));
		if (book.getFavoured().contains(user)) {
			book.getFavoured().remove(user);
			user.getFollowedBooks().remove(book);
			return false;
		} else {
			book.getFavoured().add(user);
			user.getFollowedBooks().add(book);

			String message = "User" + user.getUsername() + " favoured your book!";
			notificationService.createNotification(book.getAuthor(), message, "BOOK", book.getId());
			return true;
		}
	}

	@Override
	public BookDTO setEditorChoice(Long bookId, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(bookId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));
		existingBook.setSuggested(!existingBook.isSuggested());
		String message = "Your book has been set as editor choice!";
		notificationService.createNotification(existingBook.getAuthor(), message, "BOOK", bookId);

		return bookMapper.mapToDTO(bookRepo.save(existingBook));
	}

	@Override
	public boolean isBookLikedByUser(Long userId, Long bookId) {
		Optional<User> userOpt = userRepository.findById(userId);
		Optional<Book> bookOpt = bookRepo.findById(bookId);
		return userOpt.isPresent() && bookOpt.isPresent() && userOpt.get().getFollowedBooks().contains(bookOpt.get());
	}

	@Override
	public List<BookDTO> getTopRecentChapterBooks(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return bookMapper.mapToDTOs(bookRepo.findTopBooksWithLatestChapters(pageRequest));
	}

	@Override
	public Long getCommentCountForBook(Long bookId) {
		return commentRepository.countCommentsByBookId(bookId);
	}

	@Override
	public List<Long> getBookUploadedPerMonthNumber() {
		return bookRepo.countBooksUploadedPerMonth();
	}

}
