package com.nix.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nix.models.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
	public List<Comment> findByUserId(Integer userId);

	public List<Comment> findByBookId(Integer bookId);

	public Page<Comment> findByBookId(Integer bookId, Pageable pageable);

	public List<Comment> findByChapterId(Integer chapterId);

	public Page<Comment> findByChapterId(Integer chapterId, Pageable pageable);

	@Query("select c from Comment c where c.parentComment IS NULL")
	public List<Comment> findParentComments();

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.book.id = :bookId AND c.chapter.id IS NULL")
	public Page<Comment> findParentCommentsByBookId(@Param("bookId") Integer bookId, Pageable pageable);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.chapter.id = :chapterId")
	public Page<Comment> findParentCommentsByChapterId(@Param("chapterId") Integer chapterId, Pageable pageable);

	@Query("SELECT c FROM Comment c WHERE c.parentComment IS NULL AND c.post.id = :postId")
	public Page<Comment> findParentCommentsByPostId(@Param("postId") Integer postId, Pageable pageable);

	public Page<Comment> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

	@Query("SELECT COUNT(c) FROM Comment c WHERE c.book.id = :bookId")
	public Long countCommentsByBookId(@Param("bookId") Integer bookId);
}
