package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.SensitiveWord;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Integer> {
	
	@Query("select sw from SensitiveWord sw where sw.word LIKE %:word%")
	public List<SensitiveWord> findByWord(String word);
}
