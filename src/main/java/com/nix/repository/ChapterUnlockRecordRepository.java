package com.nix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.ChapterUnlockRecord;

public interface ChapterUnlockRecordRepository extends JpaRepository<ChapterUnlockRecord, Integer> {
	Optional<ChapterUnlockRecord> findByUserIdAndChapterId(Integer userId, Integer chapterId);
	
	@Query("SELECT cur.chapter.id FROM ChapterUnlockRecord cur WHERE cur.user.id = :userId")
    List<Integer> findChapterIdsByUserId(Integer userId);
}
