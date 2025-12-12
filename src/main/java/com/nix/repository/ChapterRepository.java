package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Book;
import com.nix.models.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
	public Chapter findByTitle(String title);

	public List<Chapter> findByBookIdOrderByUploadDateAsc(UUID bookId);

	public List<Chapter> findByBookId(UUID bookId, Sort sort);

	@Query("select c from Chapter c where c.isDraft=false and c.book.id=:bookId order by c.uploadDate asc")
	public List<Chapter> findNotDraftedChaptersByBookId(@Param("bookId") UUID bookId);

	@Query("select c from Chapter c where c.isDraft=false and c.book.id=:bookId")
	public List<Chapter> findNotDraftedChaptersByBookId(@Param("bookId") UUID bookId, Sort sort);

	Optional<Chapter> findByIdAndIsLocked(UUID id, boolean isLocked);

	@Query("select c.book b from Chapter c order by c.uploadDate DESC limit 5 ")
	public List<Book> findTopByOrderByUploadDateDesc();

	Optional<Chapter> findByRoomId(String roomId);

	@Query("SELECT c FROM Chapter c LEFT JOIN c.unlockRecords ur GROUP BY c ORDER BY COUNT(ur) DESC")
	List<Chapter> findMostUnlockedChapters(Pageable pageable);

	@Query("SELECT COUNT(ur) FROM ChapterUnlockRecord ur")
	long getTotalUnlocks();

	@Query("SELECT c FROM Chapter c WHERE c.book.author.id = :authorId ORDER BY (SELECT COUNT(ur) FROM ChapterUnlockRecord ur WHERE ur.chapter = c) DESC")
	List<Chapter> findPopularChaptersByAuthor(@Param("authorId") UUID authorId, Pageable pageable);
}
