package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Post implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	private UUID id;

	// The user who created the post
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(columnDefinition = "TEXT")
	private String content;

	@ElementCollection
	@CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
	@AttributeOverrides({
			@AttributeOverride(name = "url", column = @Column(name = "image_url")),
			@AttributeOverride(name = "isMild", column = @Column(name = "is_mild"))
	})
	private List<ImageAttachment> images = new ArrayList<>();

	private Integer likes = 0;

	private LocalDateTime timestamp;

	@JsonIgnore
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "post_likes", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private List<User> likedUsers = new ArrayList<>();

	// New field to reference the original shared post
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shared_post_id")
	private Post sharedPost;

	@ManyToOne
	@JoinColumn(name = "shared_book_id")
	private Book sharedBook;

	@ManyToOne
	@JoinColumn(name = "shared_chapter_id")
	private Chapter sharedChapter;

	@Enumerated(EnumType.STRING)
	private PostType postType;

	// Define PostType enum
	public enum PostType {
		STANDARD, BOOK_SHARE, CHAPTER_SHARE
	}
}
