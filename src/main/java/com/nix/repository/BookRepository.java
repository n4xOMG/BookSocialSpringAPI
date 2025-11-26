package com.nix.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Book;
import com.nix.models.Category;

public interface BookRepository extends JpaRepository<Book, UUID> {

	// Fetch the number of books uploaded in each month (grouped by year and month)
	@Query("SELECT COUNT(b) " + "FROM Book b " + "GROUP BY YEAR(b.uploadDate), MONTH(b.uploadDate) "
			+ "ORDER BY YEAR(b.uploadDate), MONTH(b.uploadDate)")
	List<Long> countBooksUploadedPerMonth();

	long count();

	@Query("select b from Book b where b.title LIKE %:title%")
	public List<Book> findByTitle(String title);

	@Query("SELECT b FROM Book b LEFT JOIN b.favourites fav GROUP BY b.id ORDER BY COUNT(fav) DESC")
	public List<Book> findTopBooksByLikes(Pageable pageable);

	List<Book> findByTitleContainingIgnoreCase(String title);

	Page<Book> findByAuthorId(UUID authorId, Pageable pageable);

	Page<Book> findByAuthorIdAndTitleContainingIgnoreCase(UUID authorId, String title, Pageable pageable);

	List<Book> findByIsSuggested(Boolean isSuggested);

	List<Book> findByStatus(String status);

	Page<Book> findByCategory(Category category, Pageable pageable);

	List<Book> findByTagsIn(List<Integer> tagIds);

	@Query("SELECT b FROM Book b JOIN b.chapters c GROUP BY b ORDER BY MAX(c.uploadDate) DESC")
	List<Book> findTopBooksWithLatestChapters(Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b " + "JOIN b.tags t "
			+ "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) "
			+ "AND (:categoryId IS NULL OR b.category.id = :categoryId) " + "AND (:tagIds IS NULL OR t.id IN :tagIds)")
	Page<Book> searchBooks(@Param("title") String title, @Param("categoryId") Integer categoryId,
			@Param("tagIds") List<Integer> tagIds, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.tags t WHERE b.category.id = :categoryId "
			+ "AND b.id <> :bookId AND t.id IN :tagIds")
	List<Book> findRelatedBooks(@Param("categoryId") Integer categoryId, @Param("tagIds") List<Integer> tagIds,
			@Param("bookId") UUID bookId, Pageable pageable);

	@Query("SELECT b FROM Book b ORDER BY b.viewCount DESC")
	List<Book> findMostViewedBooks(Pageable pageable);

	@Query("SELECT b FROM Book b LEFT JOIN b.favourites fav GROUP BY b ORDER BY COUNT(fav) DESC")
	List<Book> findMostFavoriteBooks(Pageable pageable);

	@Query("SELECT b FROM Book b LEFT JOIN ChapterUnlockRecord cur ON cur.chapter.book = b GROUP BY b ORDER BY COUNT(cur) DESC")
	List<Book> findMostUnlockedBooks(Pageable pageable);

	@Query("SELECT c.name, COUNT(b) FROM Book b JOIN b.category c GROUP BY c.name ORDER BY COUNT(b) DESC")
	List<Object[]> getCategoryStats();

	// Filter books excluding blocked/blocking authors
	@Query("SELECT b FROM Book b WHERE b.author.id NOT IN :excludedAuthorIds")
	Page<Book> findAllExcludingAuthors(@Param("excludedAuthorIds") Set<UUID> excludedAuthorIds, Pageable pageable);

	@Query("SELECT b FROM Book b WHERE b.author.id = :authorId AND b.author.id NOT IN :excludedAuthorIds")
	Page<Book> findByAuthorIdExcludingAuthors(@Param("authorId") UUID authorId,
			@Param("excludedAuthorIds") Set<UUID> excludedAuthorIds, Pageable pageable);

	@Query("SELECT b FROM Book b WHERE b.category.id = :categoryId AND b.author.id NOT IN :excludedAuthorIds")
	Page<Book> findByCategoryIdExcludingAuthors(@Param("categoryId") Integer categoryId,
			@Param("excludedAuthorIds") Set<UUID> excludedAuthorIds, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.tags t " +
			"WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
			"AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
			"AND (:tagIds IS NULL OR t.id IN :tagIds) " +
			"AND b.author.id NOT IN :excludedAuthorIds")
	Page<Book> searchBooksExcludingAuthors(@Param("title") String title,
			@Param("categoryId") Integer categoryId,
			@Param("tagIds") List<Integer> tagIds,
			@Param("excludedAuthorIds") Set<UUID> excludedAuthorIds,
			Pageable pageable);

	@Query("SELECT b FROM Book b JOIN b.favourites fav " +
			"WHERE fav.user.id = :userId AND b.author.id NOT IN :excludedAuthorIds")
	Page<Book> findFollowedBooksByUserIdExcludingAuthors(@Param("userId") UUID userId,
			@Param("excludedAuthorIds") Set<UUID> excludedAuthorIds, Pageable pageable);

}
