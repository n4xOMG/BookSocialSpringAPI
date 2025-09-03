package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.models.Tag;

public interface TagService {
	public Tag findTagById(Integer id);

	public List<Tag> findTagByName(String name);

	public List<Tag> findAllTags();

	public List<Tag> findALlTagsByBookId(UUID bookId);

	public Tag addNewTag(Tag category);

	public Tag editTag(Integer categoryId, Tag category);

	public String deleteTag(Integer categoryId);
}
