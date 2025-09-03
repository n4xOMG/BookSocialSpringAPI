package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.models.Category;

public interface CategoryService {
	public Category findCategoryById(Integer id);

	public List<Category> findCategoryByName(String name);

	public List<Category> findAllCategories();
	
	public List<Category> findALlCategoriesByBookId(UUID bookId);

	public Category addNewCategory(Category category);

	public Category editCategory(Integer categoryId, Category category);

	public String deleteCategory(Integer categoryId);
}
