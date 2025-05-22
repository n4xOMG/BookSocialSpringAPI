package com.nix.service;

import java.util.List;

import com.nix.models.Category;

public interface CategoryService {
	public Category findCategoryById(Integer id);

	public List<Category> findCategoryByName(String name);

	public List<Category> findAllCategories();
	
	public List<Category> findALlCategoriesByBookId(Long bookId);

	public Category addNewCategory(Category category);

	public Category editCategory(Integer categoryId, Category category);

	public String deleteCategory(Integer categoryId);
}
