package com.nix.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Book;
import com.nix.models.Category;

public interface BookRepository extends JpaRepository<Book, Integer> {

	@Query("select b from Book b where b.title LIKE %:title%")
	public List<Book> findByTitle(String title);

	@Query("select b from Book b join b.favoured u where u.id = :userId")
	public List<Book> findByUserFavoured(@Param("userId") Integer userId);

	@Query("SELECT b FROM Book b LEFT JOIN b.favoured u GROUP BY b.id ORDER BY COUNT(u) DESC LIMIT 10")
	public List<Book> findTopBooksByLikes();

	List<Book> findByTitleContainingIgnoreCase(String title);

	Page<Book> findByAuthorId(Integer authorId, Pageable pageable);

	List<Book> findByIsSuggested(Boolean isSuggested);

	List<Book> findByStatus(String status);

	Page<Book> findByCategory(Category category, Pageable pageable);

	@Query("select b from Book b join b.favoured u where u.id = :userId")
	Page<Book> findByUserFavoured(@Param("userId") Integer userId, Pageable pageable);

	List<Book> findByTagsIn(List<Integer> tagIds);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.chapters c ORDER BY c.uploadDate DESC")
	List<Book> findTopBooksWithLatestChapters(Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b " + "JOIN b.tags t "
			+ "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) "
			+ "AND (:categoryId IS NULL OR b.category.id = :categoryId) " + "AND (:tagIds IS NULL OR t.id IN :tagIds)")
	Page<Book> searchBooks(@Param("title") String title, @Param("categoryId") Integer categoryId,
			@Param("tagIds") List<Integer> tagIds, Pageable pageable);

	@Query("SELECT DISTINCT b FROM Book b JOIN b.tags t WHERE b.category.id = :categoryId "
			+ "AND b.id <> :bookId AND t.id IN :tagIds")
	List<Book> findRelatedBooks(@Param("categoryId") Integer categoryId, @Param("tagIds") List<Integer> tagIds,
			@Param("bookId") Integer bookId, Pageable pageable);

}
