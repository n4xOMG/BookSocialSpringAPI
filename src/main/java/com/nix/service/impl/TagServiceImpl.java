package com.nix.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.models.Book;
import com.nix.models.Tag;
import com.nix.repository.BookRepository;
import com.nix.repository.TagRepository;
import com.nix.service.TagService;

@Service
public class TagServiceImpl implements TagService {

	@Autowired
	TagRepository tagRepository;

	@Autowired
	BookRepository bookRepo;

	@Override
	public Tag findTagById(Integer id) {
		return tagRepository.findById(id).orElseThrow(() -> new RuntimeException("Cannot find tag with id"));
	}

	@Override
	public List<Tag> findTagByName(String name) {
		return tagRepository.findByName(name);
	}

	@Override
	public List<Tag> findAllTags() {
		return tagRepository.findAll();
	}

	@Override
	public List<Tag> findALlTagsByBookId(UUID bookId) {
		return tagRepository.findByBookId(bookId);
	}

	@Override
	public Tag addNewTag(Tag tag) {
		return tagRepository.save(tag);
	}

	@Override
	public Tag editTag(Integer tagId, Tag tag) {
		try {
			Tag editTag = findTagById(tagId);

			if (tag.getName() != null) {
				editTag.setName(tag.getName());
			}
			return tagRepository.save(editTag);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String deleteTag(Integer tagId) {
		try {
			Tag deleteTag = findTagById(tagId);

			for (Book book : deleteTag.getBooks()) {
				book.getTags().remove(deleteTag);
				deleteTag.getBooks().remove(book);

				bookRepo.save(book);
			}
			tagRepository.delete(deleteTag);
			return "Tag deleted";

		} catch (Exception e) {
			return "Failed to delete tag" + e;
		}
	}

}
