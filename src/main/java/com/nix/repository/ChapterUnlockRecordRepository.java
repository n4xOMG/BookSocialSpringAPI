package com.nix.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.ChapterUnlockRecord;

public interface ChapterUnlockRecordRepository extends JpaRepository<ChapterUnlockRecord, Integer> {
	Optional<ChapterUnlockRecord> findByUserIdAndChapterId(Integer userId, Integer chapterId);
}
