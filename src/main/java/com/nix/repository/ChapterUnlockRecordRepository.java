package com.nix.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.ChapterUnlockRecord;

public interface ChapterUnlockRecordRepository extends JpaRepository<ChapterUnlockRecord, UUID> {
	Optional<ChapterUnlockRecord> findByUserIdAndChapterId(UUID userId, UUID chapterId);

	List<ChapterUnlockRecord> findByChapterId(UUID chapterId);

	@Query("SELECT cur.chapter.id FROM ChapterUnlockRecord cur WHERE cur.user.id = :userId")
	List<UUID> findChapterIdsByUserId(UUID userId);
}
