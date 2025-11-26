package com.nix.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.dtos.CategoryDTO;
import com.nix.dtos.mappers.CategoryMapper;
import com.nix.models.Category;
import com.nix.models.User;
import com.nix.response.ApiResponseWithData;
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
	public ResponseEntity<ApiResponseWithData<List<CategoryDTO>>> getAllCategories() {
		List<Category> categories = categoryService.findAllCategories();
		List<CategoryDTO> categoryDTOs = categoryMapper.mapToDTOs(categories);
		ApiResponseWithData<List<CategoryDTO>> response = new ApiResponseWithData<>(
				"Categories retrieved successfully.", true, categoryDTOs);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/categories/{categoryId}")
	public ResponseEntity<ApiResponseWithData<CategoryDTO>> getCategoryById(@PathVariable Integer categoryId) {
		try {
			Category category = categoryService.findCategoryById(categoryId);
			ApiResponseWithData<CategoryDTO> response = new ApiResponseWithData<>(
					"Category retrieved successfully.", true, categoryMapper.mapToDTO(category));
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@GetMapping("/books/{bookId}/categories")
	public ResponseEntity<ApiResponseWithData<List<CategoryDTO>>> getCategoriesByBook(@PathVariable UUID bookId)
			throws Exception {
		try {
			List<Category> categories = categoryService.findALlCategoriesByBookId(bookId);
			List<CategoryDTO> categoryDTOs = categoryMapper.mapToDTOs(categories);
			ApiResponseWithData<List<CategoryDTO>> response = new ApiResponseWithData<>(
					"Categories retrieved successfully.", true, categoryDTOs);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PostMapping("/admin/categories")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CategoryDTO>> addNewCategory(@RequestHeader("Authorization") String jwt,
			@RequestBody Category category) throws Exception {
		try {
			Category newCategory = categoryService.addNewCategory(category);
			CategoryDTO newCategoryDTO = categoryMapper.mapToDTO(newCategory);
			ApiResponseWithData<CategoryDTO> response = new ApiResponseWithData<>(
					"Category created successfully.", true, newCategoryDTO);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@PutMapping("/admin/categories/{categoryId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<CategoryDTO>> editCategory(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer categoryId, @RequestBody Category category) throws Exception {
		try {
			Category editCategory = categoryService.editCategory(categoryId, category);
			CategoryDTO editCategoryDTO = categoryMapper.mapToDTO(editCategory);
			ApiResponseWithData<CategoryDTO> response = new ApiResponseWithData<>(
					"Category updated successfully.", true, editCategoryDTO);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}

	@DeleteMapping("/admin/categories/{categoryId}")
	@PreAuthorize("hasAnyRole('ADMIN')")
	public ResponseEntity<ApiResponseWithData<String>> deleteTag(@RequestHeader("Authorization") String jwt,
			@PathVariable Integer categoryId) throws Exception {
		try {
			String result = categoryService.deleteCategory(categoryId);
			ApiResponseWithData<String> response = new ApiResponseWithData<>(
					"Category deleted successfully.", true, result);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponseWithData<>(e.getMessage(), false));
		}
	}
}
