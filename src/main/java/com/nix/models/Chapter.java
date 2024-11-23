package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String chapterNum;
	private String title;

	@Column(columnDefinition = "TEXT")
	private String content;

	private LocalDateTime uploadDate;
	private int price;
	private boolean isLocked;
	private boolean isDeleted;
	private int viewCount;
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "book_id")
	private Book book;

	@OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReadingProgress> progresses = new ArrayList<>();

	@OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChapterUnlockRecord> unlockRecords = new ArrayList<>();

}
