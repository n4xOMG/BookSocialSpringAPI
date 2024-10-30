package com.nix.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.models.Tag;

public interface BookRepository extends JpaRepository<Book, Integer> {

	@Query("select b from Book b where b.title LIKE %:title%")
	public List<Book> findByTitle(String title);

	@Query("SELECT b FROM Book b WHERE b.author.id = :authorId")
	public List<Book> findBooksByAuthor(@Param("authorId") Integer authorId);

	@Query("select b from Book b join b.favoured u where u.id = :userId")
	public List<Book> findByUserFavoured(@Param("userId") Integer userId);

	@Query("SELECT b FROM Book b JOIN b.categories c " + "WHERE c IN :categories " + "GROUP BY b.id ")
	public List<Book> findBooksWithAllCategories(@Param("categories") List<Category> categories);

	@Query("SELECT b FROM Book b JOIN b.tags t " + "WHERE t IN :tags " + "GROUP BY b.id")
	public List<Book> findBooksByTags(@Param("tags") List<Tag> tags);

	@Query("SELECT b FROM Book b WHERE b.uploadDate >= :startDate ORDER BY b.uploadDate DESC")
	public List<Book> findRecentBooks(@Param("startDate") LocalDateTime startDate);

	@Query("SELECT b FROM Book b ORDER BY b.viewCount DESC")
	public List<Book> findTopBooksByViews();

	@Query("SELECT b FROM Book b JOIN b.favoured u GROUP BY b.id ORDER BY COUNT(u) DESC")
	public List<Book> findTopBooksByLikes();

	@Query("SELECT b FROM Book b WHERE b.isSuggested = true")
	public List<Book> findEditorSuggestedBooks();

	@Query("SELECT b FROM Book b " + "LEFT JOIN b.categories c " + "LEFT JOIN b.tags t "
			+ "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) "
			+ "AND (:categoryIds IS NULL OR c.id IN :categoryIds) " + "AND (:tagIds IS NULL OR t.id IN :tagIds) "
			+ "GROUP BY b.id")
	public List<Book> findBooksBySearchCriteria(@Param("title") String title,
			@Param("categoryIds") List<Integer> categoryIds, @Param("tagIds") List<Integer> tagIds);

	@Modifying
	@Transactional
	@Query("UPDATE Book b SET b.viewCount = b.viewCount + 1 WHERE b.id = :bookId")
	public void incrementViewCount(@Param("bookId") Integer bookId);

}
