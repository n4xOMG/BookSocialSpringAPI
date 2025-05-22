package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.mappers.CategoryMapper;
import com.nix.models.Category;
import com.nix.models.User;
import com.nix.service.CategoryService;
import com.nix.service.UserService;

@RestController
public class CategoryController {

	@Autowired
	CategoryService categoryService;

	@Autowired
	UserService userService;

	@Autowired
	CategoryMapper categoryMapper;

	@GetMapping("/categories")
	public ResponseEntity<?> getAllCategories() {
		List<Category> categories = categoryService.findAllCategories();
		return ResponseEntity.ok(categoryMapper.mapToDTOs(categories));
	}

	@GetMapping("/categories/{categoryId}")
	public ResponseEntity<?> getCategoryById(@PathVariable Integer categoryId) {
		try {
			Category category = categoryService.findCategoryById(categoryId);
			return ResponseEntity.ok(categoryMapper.mapToDTO(category));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/books/{bookId}/categories")
	public ResponseEntity<?> getCategoriesByBook(@PathVariable Long bookId) throws Exception {
		try {
			List<Category> categories = categoryService.findALlCategoriesByBookId(bookId);

			return ResponseEntity.ok(categoryMapper.mapToDTOs(categories));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/admin/categories")
	public ResponseEntity<?> addNewCategory(@RequestHeader("Authorization") String jwt, @RequestBody Category category)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			Category newCategory = categoryService.addNewCategory(category);

			return ResponseEntity.ok(categoryMapper.mapToDTO(newCategory));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/admin/categories/{categoryId}")
	public ResponseEntity<?> editCategory(@RequestHeader("Authorization") String jwt, @PathVariable Integer categoryId,
			@RequestBody Category category) throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			Category editCategory = categoryService.editCategory(categoryId, category);

			return ResponseEntity.ok(categoryMapper.mapToDTO(editCategory));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/admin/categories/{categoryId}")
	public ResponseEntity<?> deleteTag(@RequestHeader("Authorization") String jwt, @PathVariable Integer categoryId)
			throws Exception {
		User user = userService.findUserByJwt(jwt);
		if (!user.getRole().getName().equals("ADMIN") && !user.getRole().getName().equals("TRANSLATOR")) {
			return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
		}
		try {
			return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
}
