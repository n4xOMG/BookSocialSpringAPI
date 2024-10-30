package com.nix.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String fullname;
	private LocalDate birthdate;
	private String gender;
	private String username;
	private String email;
	private String password;
	private Boolean isSuspended;
	@JsonIgnore
	@ManyToOne
	private Role role;

	@Column(name = "verification_code", length = 64)
	private String verificationCode;

	private Boolean isVerified;
	private String avatarUrl;

	@Column(columnDefinition = "TEXT")
	private String bio;

	private int credits;
	private boolean isBanned;
	private String banReason;

	@JsonIgnore
	@ManyToMany
	private List<Book> followedBooks = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Rating> ratings = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReadingProgress> readingProgresses = new ArrayList<>();

	@JsonIgnore
	@ManyToMany(mappedBy = "likedUsers", cascade = CascadeType.ALL)
	private List<Comment> likedComments = new ArrayList<>();

	@OneToMany(mappedBy = "sender")
	private List<FriendRequest> sentFriendRequests;

	@OneToMany(mappedBy = "receiver")
	private List<FriendRequest> receivedFriendRequests;

	@OneToMany(mappedBy = "follower")
	private List<UserFollow> following;

	@OneToMany(mappedBy = "followed")
	private List<UserFollow> followers;

	@OneToMany(mappedBy = "blocker")
	private List<UserBlock> blocking;

	@OneToMany(mappedBy = "blocked")
	private List<UserBlock> blockedBy;

	@OneToMany(mappedBy = "user")
	private List<Purchase> purchases;

	@OneToMany(mappedBy = "author")
	private List<Book> books;

	@OneToMany(mappedBy = "user")
	private List<Notification> notifications;

	@OneToMany(mappedBy = "reporter")
	private List<Report> reports;

	@OneToMany(mappedBy = "sender")
	private List<Message> sentMessages;

	@OneToMany(mappedBy = "receiver")
	private List<Message> receivedMessages;
}
