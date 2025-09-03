package com.nix.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
public class Report implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@UuidGenerator
	private UUID id;
	
	private String reason;
	private LocalDateTime reportedDate;
	private boolean isResolved;

	@ManyToOne
	private User reporter;

	@ManyToOne
	private Book book;

	@ManyToOne
	private Chapter chapter;

	@ManyToOne
	private Comment comment;

}
