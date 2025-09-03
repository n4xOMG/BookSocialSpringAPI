package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Report;

public interface ReportRepository extends JpaRepository<Report, UUID> {
	public List<Report> findByCommentId(UUID commentId);

	public List<Report> findByChapterId(UUID chapterId);

	public List<Report> findByBookId(UUID bookId);
}
