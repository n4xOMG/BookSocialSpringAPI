package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.BookDTO;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.Tag;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.CategoryRepository;
import com.nix.repository.ChapterRepository;
import com.nix.repository.TagRepository;
import com.nix.repository.UserRepository;

@Service
public class BookServiceImpl implements BookService {

	@Autowired
	BookRepository bookRepo;

	@Autowired
	ChapterRepository chaptereRepo;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	TagRepository tagRepository;

	@Autowired
	UserRepository userRepository;

	@Override
	public List<Book> getAllBooks() {
		return bookRepo.findAll();
	}

	@Override
	public Book getBookById(Integer id) {
		return bookRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
	}

	@Override
	public List<Book> getBooksByCategoryId(Integer categoryId) {
		// Fetch the category by ID
		Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
		if (!categoryOpt.isPresent()) {
			throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
		}
		Category category = categoryOpt.get();

		// Fetch books associated with the category
		return bookRepo.findByCategory(category);
	}

	@Override
	@Transactional
	public Book createBook(BookDTO bookDTO) {
		User author = userRepository.findById(bookDTO.getAuthor().getId()).orElseThrow(
				() -> new ResourceNotFoundException("User not found with ID: " + bookDTO.getAuthor().getId()));

		Book book = new Book();
		book.setAuthor(author);
		book.setTitle(bookDTO.getTitle());
		book.setAuthorName(bookDTO.getAuthorName());
		book.setArtistName(bookDTO.getArtistName());
		book.setDescription(bookDTO.getDescription());
		book.setBookCover(bookDTO.getBookCover());
		book.setLanguage(bookDTO.getLanguage());
		book.setStatus(bookDTO.getStatus());
		book.setViewCount(0);
		book.setUploadDate(LocalDateTime.now());
		book.setSuggested(false);

		Category category = categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId()));

		book.setCategory(category);
		category.getBooks().add(book);

		List<Tag> tags = tagRepository.findAllById(bookDTO.getTagIds());
		if (tags.size() != bookDTO.getTagIds().size()) {
			throw new ResourceNotFoundException("One or more tags not found.");
		}
		book.setTags(tags);
		for (Tag tag : book.getTags()) {
			tag.getBooks().add(book);
		}

		return bookRepo.save(book);
	}

	@Override
	@Transactional
	public Book updateBook(Integer id, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
		User author = userRepository.findById(bookDTO.getAuthor().getId()).orElseThrow(
				() -> new ResourceNotFoundException("User not found with ID: " + bookDTO.getAuthor().getId()));

		// Update fields
		existingBook.setAuthor(author);
		existingBook.setTitle(bookDTO.getTitle());
		existingBook.setBookCover(bookDTO.getBookCover());
		existingBook.setAuthorName(bookDTO.getAuthorName());
		existingBook.setArtistName(bookDTO.getArtistName());
		existingBook.setDescription(bookDTO.getDescription());
		existingBook.setLanguage(bookDTO.getLanguage());
		existingBook.setStatus(bookDTO.getStatus());
		// Assuming viewCount is not updated via this method
		existingBook.setSuggested(bookDTO.isSuggested());
		existingBook.setViewCount(bookDTO.getViewCount());

		Category category = categoryRepository.findById(bookDTO.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + bookDTO.getCategoryId()));
		existingBook.getCategory().getBooks().remove(existingBook);
		existingBook.setCategory(category);
		category.getBooks().add(existingBook);

		// Update Tags
		List<Tag> newTags = tagRepository.findAllById(bookDTO.getTagIds());
		if (newTags.size() != bookDTO.getTagIds().size()) {
			throw new ResourceNotFoundException("One or more tags not found.");
		}
		// Remove old associations
		for (Tag tag : existingBook.getTags()) {
			tag.getBooks().remove(existingBook);
		}
		existingBook.setTags(newTags);
		for (Tag tag : newTags) {
			tag.getBooks().add(existingBook);
		}

		return bookRepo.save(existingBook);
	}

	@Override
	@Transactional
	public void deleteBook(Integer id) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

		existingBook.getFavoured().forEach(user -> user.getLikedComments().remove(existingBook));
		existingBook.getFavoured().forEach(user -> user.getFollowedBooks().remove(existingBook));
		existingBook.getFavoured().clear();

		existingBook.getCategory().getBooks().remove(existingBook);

		// Remove associations with Tags
		for (Tag tag : existingBook.getTags()) {
			tag.getBooks().remove(existingBook);
		}
		existingBook.getTags().clear();

		bookRepo.delete(existingBook);
	}

	@Override
	public List<Book> searchBooksByTitle(String title) {
		return bookRepo.findByTitleContainingIgnoreCase(title);
	}

	@Override
	public List<Book> getBooksByAuthor(Integer authorId) {
		return bookRepo.findByAuthorId(authorId);
	}

	@Override
	public List<Book> getBooksBySuggestedStatus(Boolean isSuggested) {
		return bookRepo.findByIsSuggested(isSuggested);
	}

	@Override
	public List<Book> getBooksByStatus(String status) {
		return bookRepo.findByStatus(status);
	}

	@Override
	public List<Book> getTop10ViewedBooks() {
		return bookRepo.findTop10ByOrderByViewCountDesc();
	}

	@Override
	public List<Book> getTop10LikedBooks() {
		return bookRepo.findTopBooksByLikes();
	}

	@Override
	public List<Book> getTopRecentChapterBooks(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return bookRepo.findTopBooksWithLatestChapters(pageRequest);
	}

	@Override
	@Transactional
	public Book markAsFavouriteBook(Book book, User user) {
		if (book.getFavoured().contains(user)) {
			book.getFavoured().remove(user);
			user.getFollowedBooks().remove(book);
		} else {
			book.getFavoured().add(user);
			user.getFollowedBooks().add(book);
		}

		return book;
	}

	@Override
	public List<Book> getFollowedBooksByUserId(Integer id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		return user.getFollowedBooks();
	}

	@Override
	public Book incrementViewCount(Integer id) {
		Book book = getBookById(id);
		book.setViewCount(book.getViewCount() + 1);
		return bookRepo.save(book);
	}

	@Override
	public List<Category> getTopSixCategoriesWithBooks() {
		return categoryRepository.findTop6ByOrderByNameAsc();
	}

	@Override
	public List<Book> getFeaturedBooks() {
		return bookRepo.findByIsSuggested(true);
	}

	@Override
	public List<Book> searchBooks(String title, Integer categoryId, List<Integer> tagIds) {
		return bookRepo.searchBooks(title, categoryId, tagIds);
	}

	@Override
	public Book setEditorChoice(Integer id, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
		existingBook.setSuggested(!existingBook.isSuggested());

		return bookRepo.save(existingBook);

	}

	@Override
	public List<Book> getRelatedBooks(Integer bookId, List<Integer> tagIds) {
		Book currentBook = bookRepo.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        Integer categoryId = currentBook.getCategory().getId();

        // If tagIds are not provided, use the current book's tags
        if (tagIds == null || tagIds.isEmpty()) {
            tagIds = currentBook.getTags().stream()
                    .map(tag -> tag.getId())
                    .collect(Collectors.toList());
        }

        // Fetch related books excluding the current book
        List<Book> relatedBooks = bookRepo.findRelatedBooks(categoryId, tagIds, bookId, PageRequest.of(0, 5));

        // Map to DTOs
        return relatedBooks;
	}

}
