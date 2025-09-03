package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
	public List<Comment> findByUserId(UUID userId);

	public List<Comment> findByBookId(UUID bookId);

	public Page<Comment> findByBookId(UUID bookId, Pageable pageable);

	public List<Comment> findByChapterId(UUID chapterId);

	public Page<Comment> findByChapterId(UUID chapterId, Pageable pageable);

	@Query("select c from Comment c where c.parentComment IS NULL")
	public List<Comment> findParentComments();

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.book.id = :bookId AND c.chapter.id IS NULL")
	public Page<Comment> findParentCommentsByBookId(@Param("bookId") UUID bookId, Pageable pageable);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.chapter.id = :chapterId")
	public Page<Comment> findParentCommentsByChapterId(@Param("chapterId") UUID chapterId, Pageable pageable);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.post.id = :postId")
	public Page<Comment> findParentCommentsByPostId(@Param("postId") UUID postId, Pageable pageable);

	public Page<Comment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

	@Query("SELECT COUNT(c) FROM Comment c WHERE c.book.id = :bookId")
	public Long countCommentsByBookId(@Param("bookId") UUID bookId);
}
