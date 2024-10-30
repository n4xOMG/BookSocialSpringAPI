package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.ReadingProgress;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Integer> {
	public List<ReadingProgress> findByUserId(Integer userId);

	public ReadingProgress deleteByChapterId(Integer chapterId);

	@Query("select rp from ReadingProgress rp where rp.chapter.id=:chapterId and rp.user.id=:userId")
	public ReadingProgress findReadingProgressByChapterAndUserId(@Param(value = "chapterId") Integer chapterId,
			@Param(value = "userId") Integer userId);
	
	@Query("select rp from ReadingProgress rp where rp.chapter.book.id=:bookId and rp.user.id=:userId")
	public List<ReadingProgress> findReadingProgressByBookAndUserId(@Param(value = "bookId") Integer bookId,
			@Param(value = "userId") Integer userId);
}
