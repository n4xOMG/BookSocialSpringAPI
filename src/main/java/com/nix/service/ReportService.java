package com.nix.service;

import java.util.List;

import com.nix.dtos.ReportDTO;
import com.nix.models.Report;

public interface ReportService {
	ReportDTO createReport(String jwt, ReportDTO reportDTO) throws Exception;

	List<ReportDTO> getAllReports();

	Long getReportsCount();

	ReportDTO getReportById(Long id) throws Exception;

	void resolveReport(Long id) throws Exception;

	void deleteReport(Long id) throws Exception;

	void deleteReportedObject(Long id, String jwt) throws Exception;

	Report saveReport(Report report);
}
