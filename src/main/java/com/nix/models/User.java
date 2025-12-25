package com.nix.models;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
	@UuidGenerator
	private UUID id;

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

	// OTP expiration - OTPs are valid for a limited time
	private LocalDateTime otpExpiration;

	private Boolean isVerified;

	// Password reset token - generated after OTP verification for RESET_PASSWORD
	// context
	@Column(name = "password_reset_token", length = 36)
	private String passwordResetToken;

	private LocalDateTime passwordResetTokenExpiry;

	// Pending email - stores the new email until verified via OTP
	private String pendingEmail;

	// Previous email - for rollback capability
	private String previousEmail;

	// Token for email recovery sent to the original email
	@Column(length = 36)
	private String emailRecoveryToken;

	// Expiry for email recovery (48 hours from email change)
	private LocalDateTime emailRecoveryTokenExpiry;

	private String avatarUrl;

	@Column(columnDefinition = "TEXT")
	private String bio;

	private boolean isBanned;
	private String banReason;

	@Column(updatable = false)
	private LocalDateTime accountCreatedDate;

	private LocalDateTime suspendDate;

	private LocalDateTime banDate;

	private LocalDateTime lastLoginDate;

	@Column(columnDefinition = "TEXT")
	private String suspensionReason;

	private Integer loginAttempts;

	@JsonIgnore
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BookFavourite> favourites = new ArrayList<>();

	@JsonIgnore
	@ManyToMany
	@JoinTable(name = "user_liked_chapters", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "chapter_id"))
	private List<Chapter> likedChapters = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Rating> ratings = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReadingProgress> readingProgresses = new ArrayList<>();

	@JsonIgnore
	@ManyToMany(mappedBy = "likedUsers", cascade = CascadeType.ALL)
	private List<Comment> likedComments = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserFollow> following = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserFollow> followers = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "blocker", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserBlock> blocking = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "blocked", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserBlock> blockedBy = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Purchase> purchases = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Book> books = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Notification> notifications = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "reporter", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Report> reports = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Message> sentMessages = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Message> receivedMessages = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Post> posts = new ArrayList<>();

	@JsonIgnore
	@ManyToMany(mappedBy = "likedUsers")
	private List<Post> likedPosts = new ArrayList<>();

	@JsonIgnore
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private UserWallet wallet;
}
