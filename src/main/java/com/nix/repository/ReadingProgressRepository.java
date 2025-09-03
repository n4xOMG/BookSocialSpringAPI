package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.ReadingProgress;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, UUID> {
	public List<ReadingProgress> findByUserId(UUID userId);

	public void deleteByChapterId(UUID chapterId);

	@Query("select rp from ReadingProgress rp where rp.chapter.id=:chapterId and rp.user.id=:userId")
	public ReadingProgress findReadingProgressByChapterAndUserId(@Param(value = "chapterId") UUID chapterId,
			@Param(value = "userId") UUID userId);
	
	@Query("select rp from ReadingProgress rp where rp.chapter.book.id=:bookId and rp.user.id=:userId")
	public List<ReadingProgress> findReadingProgressByBookAndUserId(@Param(value = "bookId") UUID bookId,
			@Param(value = "userId") UUID userId);
}
