package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.CategoryDTO;
import com.nix.models.Category;

public class CategoryMapper implements Mapper<Category, CategoryDTO> {

	BookMapper bookMapper = new BookMapper();

	@Override
	public CategoryDTO mapToDTO(Category category) {
		CategoryDTO categoryDTO = new CategoryDTO();
		if (category.getId() != null) {
			categoryDTO.setId(category.getId());
		}
		categoryDTO.setDescription(category.getDescription());
		categoryDTO.setName(category.getName());
		categoryDTO.setBooks(bookMapper.mapToDTOs(category.getBooks()));
		return categoryDTO;
	}

	@Override
	public List<CategoryDTO> mapToDTOs(List<Category> categories) {
		return categories.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
