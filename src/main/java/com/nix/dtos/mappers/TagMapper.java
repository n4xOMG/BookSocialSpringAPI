package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nix.dtos.TagDTO;
import com.nix.models.Tag;

@Component
public class TagMapper implements Mapper<Tag, TagDTO> {
	@Override
	public TagDTO mapToDTO(Tag tag) {
		TagDTO tagDTO = new TagDTO();
		if (tag.getId() != null) {
			tagDTO.setId(tag.getId());
		}
		tagDTO.setName(tag.getName());

		return tagDTO;
	}

	@Override
	public List<TagDTO> mapToDTOs(List<Tag> tags) {
		return tags.stream().map(this::mapToDTO).collect(Collectors.toList());
	}
}
