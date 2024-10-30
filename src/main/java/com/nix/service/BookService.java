package com.nix.service;

import java.time.LocalDateTime;
import java.util.List;

import com.nix.models.Book;
import com.nix.models.User;

public interface BookService {
	public Book findBookById(Integer bookId) throws Exception;

	public Book getBookWithLatestChapter();

	public List<Book> findBookByTitle(String bookTitle);

	public List<Book> findBooksByAuthor(Integer authorId);

	public List<Book> findBooksFollowedByUser(Integer userId);

	public List<Book> findBooksByCategories(Integer categoryId);

	public List<Book> findBookByTags(Integer tagId);

	public List<Book> findRecentBooks(LocalDateTime starTime);

	public List<Book> findTopBooksByViews();

	public List<Book> findTopBooksByLikes();

	public List<Book> findEditorSuggestedBooks();

	public List<Book> findMostPurchaseBooks();

	public List<Book> searchBooks(String query, Integer categoryId, Integer tagId);

	public List<Book> getAllBooks();

	public Book addNewBook(Book book);

	public Book updateBook(Book book, Integer bookId) throws Exception;

	public String deleteBook(Integer bookId) throws Exception;

	public Book markAsFavouriteBook(Book book, User user);

	public void incrementBookViewCount(Integer bookId);

}
