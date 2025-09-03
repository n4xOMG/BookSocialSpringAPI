package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@UuidGenerator
	private UUID id;

	private String content;

	private LocalDateTime timestamp;
	private String imageUrl;
	private boolean isRead;
	
	@JsonIgnore
	@ManyToOne
	private User sender;
	
	@JsonIgnore
	@ManyToOne
	private User receiver;

	@JsonIgnore
	@ManyToOne
	private Chat chat;
}
