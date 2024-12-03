package com.nix.dtos.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.nix.dtos.ReportDTO;
import com.nix.models.Report;

public class ReportMapper implements Mapper<Report, ReportDTO> {

	@Override
	public ReportDTO mapToDTO(Report r) {
		if (r != null) {
			ReportDTO reportDTO = new ReportDTO();
			if (r.getId() != null) {
				reportDTO.setId(r.getId());
			}
			if (r.getBook() != null) {
				reportDTO.setBookId(r.getBook().getId());
			}
			if (r.getChapter() != null) {
				reportDTO.setChapterId(r.getChapter().getId());
			}
			if (r.getComment() != null) {
				reportDTO.setCommentId(r.getComment().getId());
			}
			reportDTO.setReason(r.getReason());
			return reportDTO;
		}
		return null;
	}

	@Override
	public List<ReportDTO> mapToDTOs(List<Report> reports) {
		return reports.stream().map(this::mapToDTO).collect(Collectors.toList());
	}

}
