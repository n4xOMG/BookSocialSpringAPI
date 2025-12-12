package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
public class Chapter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@UuidGenerator
	private UUID id;
	private String roomId;
	private boolean isDraft;
	private String chapterNum;
	private String title;

	@Column(columnDefinition = "LONGTEXT")
	private String content;

	private LocalDateTime uploadDate;
	private int price;
	private boolean isLocked;

	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "book_id")
	private Book book;

	@OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	@OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReadingProgress> progresses = new ArrayList<>();

	@OneToMany(mappedBy = "chapter")
	private List<ChapterUnlockRecord> unlockRecords = new ArrayList<>();
	@JsonIgnore
	@ManyToMany(mappedBy = "likedChapters")
	private List<User> likedUsers = new ArrayList<>();
}
