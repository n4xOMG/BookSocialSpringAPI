package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.nix.models.Book;
import com.nix.models.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
	public Chapter findByTitle(String title);

	public List<Chapter> findByBookId(Integer bookId);

	@Query("select c.book b from Chapter c order by c.uploadDate DESC limit 1 ")
	public Book findTopByOrderByUploadDateDesc();
	
	@Modifying
    @Transactional
    @Query("UPDATE Chapter c SET c.viewCount = c.viewCount + 1 WHERE c.id = :chapterId")
    void incrementViewCount(@Param("chapterId") Integer chapterId);
}