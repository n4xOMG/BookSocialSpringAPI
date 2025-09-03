package com.nix.service;

import java.util.List;
import java.util.UUID;

import com.nix.dtos.ReportDTO;
import com.nix.models.Report;

public interface ReportService {
	ReportDTO createReport(String jwt, ReportDTO reportDTO) throws Exception;

	List<ReportDTO> getAllReports();

	Long getReportsCount();

	ReportDTO getReportById(UUID id) throws Exception;

	void resolveReport(UUID id) throws Exception;

	void deleteReport(UUID id) throws Exception;

	void deleteReportedObject(UUID id, String jwt) throws Exception;

	Report saveReport(Report report);
}
