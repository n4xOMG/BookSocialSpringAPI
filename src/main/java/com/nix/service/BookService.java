package com.nix.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.BookDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.models.User;

public interface BookService {

	public Long getBookCount();

	public BookDTO getBookById(Integer id);

	public BookDTO createBook(BookDTO bookDTO) throws IOException;

	public BookDTO updateBook(Integer id, BookDTO bookDTO);

	public void deleteBook(Integer id);

	Page<BookDTO> getAllBooks(Pageable pageable);

	Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable);

	Page<BookDTO> getBooksByAuthor(Integer authorId, Pageable pageable);

	Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable);

	Page<BookDTO> getFollowedBooksByUserId(Integer id, Pageable pageable);

	public List<BookDTO> getBooksBySuggestedStatus(Boolean isSuggested);

	public List<BookDTO> getBooksByStatus(String status);

	List<CategoryDTO> getTopSixCategoriesWithBooks();

	public List<BookDTO> getTop10LikedBooks();

	List<BookDTO> getFeaturedBooks();

	public boolean markAsFavouriteBook(BookDTO book, User user);

	public BookDTO setEditorChoice(Integer id, BookDTO bookDTO);

	List<BookDTO> getTopRecentChapterBooks(int limit);

	List<BookDTO> getRelatedBooks(Integer bookId, List<Integer> tagIds);

	List<Long> getBookUploadedPerMonthNumber();

	public boolean isBookLikedByUser(Integer userId, Integer bookId);

	public Long getCommentCountForBook(Integer bookId);
}
