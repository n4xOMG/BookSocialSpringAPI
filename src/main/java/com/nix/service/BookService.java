package com.nix.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.models.User;

public interface BookService {

	public Long getBookCount();

	public BookDTO getBookById(UUID bookId);

	public BookDTO createBook(BookDTO bookDTO) throws IOException;

	public BookDTO updateBook(UUID bookId, BookDTO bookDTO);

	public void deleteBook(UUID bookId);

	Page<BookDTO> getAllBooks(Pageable pageable);

	Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable);

	Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable);

	Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable);

	Page<BookDTO> getFollowedBooksByUserId(UUID bookId, Pageable pageable);

	public List<BookDTO> getBooksBySuggestedStatus(Boolean isSuggested);

	public List<BookDTO> getBooksByStatus(String status);

	List<CategoryDTO> getTopSixCategoriesWithBooks();

	public List<BookDTO> getTop10LikedBooks();

	List<BookDTO> getFeaturedBooks();

	public boolean markAsFavouriteBook(BookDTO book, User user);

	public BookDTO setEditorChoice(UUID bookId, BookDTO bookDTO);

	List<BookDTO> getTopRecentChapterBooks(int limit);

	List<BookDTO> getRelatedBooks(UUID bookId, List<Integer> tagIds);

	List<Long> getBookUploadedPerMonthNumber();

	public boolean isBookLikedByUser(UUID userId, UUID bookId);

	public Long getCommentCountForBook(UUID bookId);
}
