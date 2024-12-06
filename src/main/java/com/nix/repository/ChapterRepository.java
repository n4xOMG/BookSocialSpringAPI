package com.nix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.Book;
import com.nix.models.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
	public Chapter findByTitle(String title);

	public List<Chapter> findByBookId(Integer bookId);

	@Query("select c from Chapter c where c.isDraft=false and c.book.id=:bookId")
	public List<Chapter> findNotDraftedChaptersByBookId(Integer bookId);

	Optional<Chapter> findByIdAndIsLocked(Integer id, boolean isLocked);

	@Query("select c.book b from Chapter c order by c.uploadDate DESC limit 5 ")
	public List<Book> findTopByOrderByUploadDateDesc();


	Optional<Chapter> findByRoomId(String roomId);
}
