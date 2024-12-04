package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nix.models.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
	public List<Report> findByCommentId(Integer commentId);

	public List<Report> findByChapterId(Integer chapterId);

	public List<Report> findByBookId(Integer bookId);
}
