package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nix.dtos.ReportDTO;
import com.nix.models.Report;

@Component
public class ReportMapper implements Mapper<Report, ReportDTO> {
	@Autowired
	BookMapper bookMapper;

	CommentMapper commentMapper = new CommentMapper();

	ChapterSummaryMapper chapterMapper = new ChapterSummaryMapper();

	UserSummaryMapper userSummaryMapper = new UserSummaryMapper();

	@Override
	public ReportDTO mapToDTO(Report r) {
		if (r != null) {
			ReportDTO reportDTO = new ReportDTO();
			if (r.getId() != null) {
				reportDTO.setId(r.getId());
			}
			if (r.getBook() != null) {
				reportDTO.setBook(bookMapper.mapToDTO(r.getBook()));
			}
			if (r.getChapter() != null) {
				reportDTO.setChapter(chapterMapper.mapToDTO(r.getChapter()));
			}
			if (r.getComment() != null) {
				reportDTO.setComment(commentMapper.mapToDTO(r.getComment()));
			}
			reportDTO.setReporter(userSummaryMapper.mapToDTO(r.getReporter()));
			reportDTO.setReportedDate(r.getReportedDate());
			reportDTO.setReason(r.getReason());
			reportDTO.setResolved(r.isResolved());
			return reportDTO;
		}
		return null;
	}

	@Override
	public List<ReportDTO> mapToDTOs(List<Report> reports) {
		return reports.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
