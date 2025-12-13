package com.nix.dtos.mappers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nix.dtos.BookDTO;
import com.nix.models.Book;
import com.nix.models.Chapter;
import com.nix.models.Rating;
import com.nix.models.Tag;
import com.nix.repository.BookFavouriteRepository;

@Component
public class BookMapper implements Mapper<Book, BookDTO> {
	CommentMapper commentMapper = new CommentMapper();

	RatingMapper ratingMapper = new RatingMapper();

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Autowired
	private BookFavouriteRepository bookFavouriteRepository;

	@Override
	public BookDTO mapToDTO(Book book) {
		BookDTO bookDTO = new BookDTO();
		if (book.getId() != null) {
			bookDTO.setId(book.getId());
		}
		bookDTO.setTitle(book.getTitle());
		bookDTO.setAuthor(userSummaryMapper.mapToDTO(book.getAuthor()));
		bookDTO.setAuthorName(book.getAuthorName());
		bookDTO.setArtistName(book.getArtistName());
		if (book.getBookCover() != null) {
			bookDTO.setBookCover(
					new com.nix.dtos.ImageAttachmentDTO(book.getBookCover().getUrl(), book.getBookCover().getIsMild()));
		}
		bookDTO.setDescription(book.getDescription());
		bookDTO.setUploadDate(book.getUploadDate());
		bookDTO.setSuggested(book.isSuggested());
		bookDTO.setStatus(book.getStatus());
		bookDTO.setViewCount(book.getViewCount());
		if (book.getRatings() != null && !book.getRatings().isEmpty()) {
			double avgRating = book.getRatings().stream().mapToDouble(Rating::getRating).average().orElse(0.0);
			bookDTO.setAvgRating(avgRating);
			bookDTO.setRatingCount(book.getRatings().size());
		} else {
			bookDTO.setAvgRating(0.0);
			bookDTO.setRatingCount(0);
		}
		long favCount = bookFavouriteRepository.countByBookId(book.getId());
		bookDTO.setFavCount(Math.toIntExact(favCount));

		bookDTO.setChapterCount(book.getChapters().size());
		bookDTO.setLanguage(book.getLanguage());
		book.getChapters().stream().max(Comparator.comparing(Chapter::getUploadDate))
				.ifPresent(latestChapter -> bookDTO.setLatestChapterNumber(latestChapter.getChapterNum()));
		// Map categoryIds
		if (book.getCategory() != null) {
			bookDTO.setCategoryId(book.getCategory().getId());
			bookDTO.setCategoryName(book.getCategory().getName());
		}

		// Map tagIds and tagNames
		if (book.getTags() != null && !book.getTags().isEmpty()) {
			List<Integer> tagIds = book.getTags().stream().map(Tag::getId).collect(Collectors.toList());
			bookDTO.setTagIds(tagIds);
			List<String> tagNames = book.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toList());
			bookDTO.setTagNames(tagNames);
		}

		return bookDTO;
	}

	@Override
	public List<BookDTO> mapToDTOs(List<Book> books) {
		return books.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
