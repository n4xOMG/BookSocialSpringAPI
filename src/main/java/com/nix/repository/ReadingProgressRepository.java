package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.ReadingProgress;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {
	public List<ReadingProgress> findByUserId(Long userId);

	public void deleteByChapterId(Long chapterId);

	@Query("select rp from ReadingProgress rp where rp.chapter.id=:chapterId and rp.user.id=:userId")
	public ReadingProgress findReadingProgressByChapterAndUserId(@Param(value = "chapterId") Long chapterId,
			@Param(value = "userId") Long userId);
	
	@Query("select rp from ReadingProgress rp where rp.chapter.book.id=:bookId and rp.user.id=:userId")
	public List<ReadingProgress> findReadingProgressByBookAndUserId(@Param(value = "bookId") Long bookId,
			@Param(value = "userId") Long userId);
}
