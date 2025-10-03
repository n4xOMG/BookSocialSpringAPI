package com.nix.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.models.Book;
import com.nix.models.Category;
import com.nix.repository.BookRepository;
import com.nix.repository.CategoryRepository;
import com.nix.service.CategoryService;

@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	CategoryRepository categoryRepo;

	@Autowired
	BookRepository bookRepo;

	@Override
	public Category findCategoryById(Integer id) {
		return categoryRepo.findById(id).orElseThrow(() -> new RuntimeException("Cannot find category with id"));
	}

	@Override
	public List<Category> findCategoryByName(String name) {
		return categoryRepo.findByName(name);
	}

	@Override
	public List<Category> findAllCategories() {
		return categoryRepo.findAll();
	}

	@Override
	public Category addNewCategory(Category category) {
		return categoryRepo.save(category);
	}

	@Override
	public Category editCategory(Integer categoryId, Category category) {
		try {
			Category editCategory = findCategoryById(categoryId);

			if (category.getName() != null) {
				editCategory.setName(category.getName());
			}
			if (category.getDescription() != null) {
				editCategory.setDescription(category.getDescription());
			}

			return categoryRepo.save(editCategory);
		} catch (Exception e) {
			return null;
		}
	}

	public String deleteCategory(Integer categoryId) {
		try {
			Category deleteCategory = findCategoryById(categoryId);

			List<Book> booksToRemove = new ArrayList<>(deleteCategory.getBooks());

			for (Book book : booksToRemove) {
				book.setCategory(null);

				bookRepo.save(book);
			}

			categoryRepo.delete(deleteCategory);

			return "Category deleted";

		} catch (Exception e) {
			return "Failed to delete category: " + e.getMessage();
		}
	}

	@Override
	public List<Category> findALlCategoriesByBookId(UUID bookId) {
		// TODO Auto-generated method stub
		return null;
	}

}
