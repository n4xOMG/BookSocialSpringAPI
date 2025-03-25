package com.nix.service;

import java.util.List;

import com.nix.dtos.BookDTO;
import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.User;

public interface BookService {
	public List<Book> getAllBooks();

	public Book getBookById(Integer id);

	public Book createBook(BookDTO bookDTO);

	public Book updateBook(Integer id, BookDTO bookDTO);

	public void deleteBook(Integer id);

	public List<Book> searchBooksByTitle(String title);

	public List<Book> searchBooks(String title, Integer categoryId, List<Integer> tagIds);

	public List<Book> getBooksByAuthor(Integer authorId);

	public List<Book> getBooksBySuggestedStatus(Boolean isSuggested);

	public List<Book> getBooksByStatus(String status);

	List<Book> getBooksByCategoryId(Integer categoryId);

	List<Category> getTopSixCategoriesWithBooks();

	public List<Book> getTop10LikedBooks();

	List<Book> getFeaturedBooks();

	public boolean markAsFavouriteBook(Book book, User user);

	public Book setEditorChoice(Integer id, BookDTO bookDTO);

	List<Book> getFollowedBooksByUserId(Integer id);

	List<Book> getTopRecentChapterBooks(int limit);

	List<Book> getRelatedBooks(Integer bookId, List<Integer> tagIds);
	
	public boolean isBookLikedByUser(Integer userId, Integer bookId);
}
