package com.nix.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.dtos.BookDTO;
import com.nix.dtos.BookPerformanceDTO;
import com.nix.dtos.CategoryDTO;
import com.nix.models.User;

public interface BookService {

	public Long getBookCount();

	public BookDTO getBookById(UUID bookId);

	public BookDTO createBook(BookDTO bookDTO, UUID authorId) throws IOException;

	public BookDTO updateBook(UUID bookId, BookDTO bookDTO);

	public void deleteBook(UUID bookId);

	Page<BookDTO> getAllBooks(Pageable pageable);

	// Overloaded methods with author exclusion for efficient filtering
	Page<BookDTO> getAllBooks(Pageable pageable, Set<UUID> excludedAuthorIds);

	Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable);

	Page<BookDTO> getBooksByCategoryId(Integer categoryId, Pageable pageable, Set<UUID> excludedAuthorIds);

	Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable);

	Page<BookDTO> getBooksByAuthor(UUID authorId, Pageable pageable, Set<UUID> excludedAuthorIds);

	Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable);

	Page<BookDTO> searchBooks(String title, Integer categoryId, List<Integer> tagIds, Pageable pageable,
			Set<UUID> excludedAuthorIds);

	Page<BookDTO> searchBooksForAuthor(UUID authorId, String query, Pageable pageable);

	Page<BookDTO> getFollowedBooksByUserId(UUID userId, Pageable pageable);

	Page<BookDTO> getFollowedBooksByUserId(UUID userId, Pageable pageable, Set<UUID> excludedAuthorIds);

	Set<UUID> getFavouriteBookIdsForUser(UUID userId);

	public List<BookDTO> getBooksBySuggestedStatus(Boolean isSuggested);

	public List<BookDTO> getBooksByStatus(String status);

	List<CategoryDTO> getTopSixCategoriesWithBooks();

	public List<BookDTO> getTop10LikedBooks();

	List<BookDTO> getFeaturedBooks();

	public boolean markAsFavouriteBook(UUID bookId, User user);

	long recordBookView(UUID bookId, UUID viewerId, String viewerIp);

	public BookDTO setEditorChoice(UUID bookId, BookDTO bookDTO);

	List<BookDTO> getTopRecentChapterBooks(int limit);

	List<BookDTO> getRelatedBooks(UUID bookId, List<Integer> tagIds);

	List<Long> getBookUploadedPerMonthNumber();

	public boolean isBookLikedByUser(UUID userId, UUID bookId);

	public Long getCommentCountForBook(UUID bookId);

	// Trending books functionality
	List<BookDTO> getTrendingBooks(int hours, long minViews, int limit);

	// Book performance for author dashboard
	List<BookPerformanceDTO> getAuthorBookPerformance(UUID authorId);
}
