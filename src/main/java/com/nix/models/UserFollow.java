package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "follower_id", "followed_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@UuidGenerator
	private UUID id;
	private LocalDateTime followDate;

	@ManyToOne
	private User follower;

	@ManyToOne
	private User followed;

}