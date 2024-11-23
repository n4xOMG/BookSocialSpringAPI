package com.nix.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nix.models.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
	@Query("select c from Category c where c.name LIKE %:name%")
	public List<Category> findByName(String name);
	
	@Query("select c from Category c join c.books b where b.id = :bookId")
	public List<Category> findByBookId(Integer bookId);
	
	List<Category> findTop6ByOrderByNameAsc();
}
