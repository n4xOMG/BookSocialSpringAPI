package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nix.models.Book;
import com.nix.models.BookFavourite;
import com.nix.models.User;

public interface BookFavouriteRepository extends JpaRepository<BookFavourite, UUID> {
	Optional<BookFavourite> findByBookAndUser(Book book, User user);

	List<BookFavourite> findByUserId(UUID userId);

	Page<BookFavourite> findByUserId(UUID userId, Pageable pageable);

	List<BookFavourite> findByBookId(UUID bookId);

	Long countByBookId(UUID bookId);

	boolean existsByBookIdAndUserId(UUID bookId, UUID userId);

	@Query("SELECT bf.book.id FROM BookFavourite bf WHERE bf.user.id = :userId")
	List<UUID> findBookIdsByUserId(@Param("userId") UUID userId);
}
