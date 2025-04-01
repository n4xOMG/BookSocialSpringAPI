package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nix.dtos.BookDTO;
import com.nix.dtos.mappers.BookMapper;
import com.nix.exception.ResourceNotFoundException;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.User;
import com.nix.repository.BookRepository;
import com.nix.repository.CategoryRepository;
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
	BookMapper bookMapper;

	@Override
	public List<BookDTO> getAllBooks() {
		return bookMapper.mapToDTOs(bookRepo.findAll());
	}

	@Override
	public BookDTO getBookById(Integer id) {
		Book book = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
		return bookMapper.mapToDTO(book);
	}

	@Override
	public List<BookDTO> getBooksByCategoryId(Integer categoryId) {
		Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
		if (!categoryOpt.isPresent()) {
			throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
		}
		return bookMapper.mapToDTOs(bookRepo.findByCategory(categoryOpt.get()));
	}

	@Override
	@Transactional
	public BookDTO createBook(BookDTO bookDTO) {
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
	public BookDTO updateBook(Integer id, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
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
	public void deleteBook(Integer id) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
		bookRepo.delete(existingBook);
	}

	@Override
	public List<BookDTO> searchBooksByTitle(String title) {
		return bookMapper.mapToDTOs(bookRepo.findByTitleContainingIgnoreCase(title));
	}

	@Override
	public List<BookDTO> getBooksByAuthor(Integer authorId) {
		return bookMapper.mapToDTOs(bookRepo.findByAuthorId(authorId));
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
	public List<BookDTO> getRelatedBooks(Integer bookId, List<Integer> tagIds) {
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
	public List<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds) {
		return bookMapper.mapToDTOs(bookRepo.searchBooks(title, categoryId, tagIds));
	}

	@Override
	public List<BookDTO> getBooksByStatus(String status) {
		return bookMapper.mapToDTOs(bookRepo.findByStatus(status));
	}

	@Override
	public List<Category> getTopSixCategoriesWithBooks() {
		return categoryRepository.findTop6ByOrderByNameAsc();
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
			return true;
		}
	}

	@Override
	public List<BookDTO> getFollowedBooksByUserId(Integer id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		return bookMapper.mapToDTOs(user.getFollowedBooks());
	}

	@Override
	public BookDTO setEditorChoice(Integer id, BookDTO bookDTO) {
		Book existingBook = bookRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
		existingBook.setSuggested(!existingBook.isSuggested());
		return bookMapper.mapToDTO(bookRepo.save(existingBook));
	}

	@Override
	public boolean isBookLikedByUser(Integer userId, Integer bookId) {
		Optional<User> userOpt = userRepository.findById(userId);
		Optional<Book> bookOpt = bookRepo.findById(bookId);
		return userOpt.isPresent() && bookOpt.isPresent() && userOpt.get().getFollowedBooks().contains(bookOpt.get());
	}

	@Override
	public List<BookDTO> getTopRecentChapterBooks(int limit) {
		PageRequest pageRequest = PageRequest.of(0, limit);
		return bookMapper.mapToDTOs(bookRepo.findTopBooksWithLatestChapters(pageRequest));
	}
}
