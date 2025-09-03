package com.nix.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.Tag;

public interface TagRepository extends JpaRepository<Tag, Integer>{
	@Query("select t from Tag t where t.name LIKE %:name%")
	public List<Tag> findByName(String name);
	
	@Query("select t from Tag t join t.books b where b.id = :bookId")
	public List<Tag> findByBookId(UUID bookId);
}
