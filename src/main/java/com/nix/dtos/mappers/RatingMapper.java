package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.RatingDTO;
import com.nix.models.Rating;

public class RatingMapper implements Mapper<Rating, RatingDTO> {

	@Override
	public RatingDTO mapToDTO(Rating rating) {
		RatingDTO ratingDTO = new RatingDTO();
		if (rating != null) {
			if (rating.getId() != null) {
				ratingDTO.setId(rating.getId());
			}
			ratingDTO.setBookId(rating.getBook().getId());
			ratingDTO.setUserId(rating.getUser().getId());
			ratingDTO.setRating(rating.getRating());
		}
		return ratingDTO;
	}

	@Override
	public List<RatingDTO> mapToDTOs(List<Rating> ratings) {
		return ratings.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
