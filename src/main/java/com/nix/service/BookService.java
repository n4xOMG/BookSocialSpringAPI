package com.nix.service;

import java.util.List;

import com.nix.dtos.BookDTO;
import com.nix.models.Category;
import com.nix.models.User;

public interface BookService {
	public List<BookDTO> getAllBooks();

	public BookDTO getBookById(Integer id);

	public BookDTO createBook(BookDTO bookDTO);

	public BookDTO updateBook(Integer id, BookDTO bookDTO);

	public void deleteBook(Integer id);

	public List<BookDTO> searchBooksByTitle(String title);

	public List<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds);

	public List<BookDTO> getBooksByAuthor(Integer authorId);

	public List<BookDTO> getBooksBySuggestedStatus(Boolean isSuggested);

	public List<BookDTO> getBooksByStatus(String status);

	List<BookDTO> getBooksByCategoryId(Integer categoryId);

	List<Category> getTopSixCategoriesWithBooks();

	public List<BookDTO> getTop10LikedBooks();

	List<BookDTO> getFeaturedBooks();

	public boolean markAsFavouriteBook(BookDTO book, User user);

	public BookDTO setEditorChoice(Integer id, BookDTO bookDTO);

	List<BookDTO> getFollowedBooksByUserId(Integer id);

	List<BookDTO> getTopRecentChapterBooks(int limit);

	List<BookDTO> getRelatedBooks(Integer bookId, List<Integer> tagIds);
	
	public boolean isBookLikedByUser(Integer userId, Integer bookId);
}
