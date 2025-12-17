package com.nix.models;

import java.io.Serializable;
import java.sql.Date;
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
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "blocker_id", "blocked_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBlock implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Id
	@UuidGenerator
	private UUID id;
	private Date blockDate;

	@ManyToOne
	private User blocker;

	@ManyToOne
	private User blocked;

}