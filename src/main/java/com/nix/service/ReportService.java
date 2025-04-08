package com.nix.service;

import java.util.List;

import com.nix.models.Report;

public interface ReportService {
	List<Report> getAllReports();

	Long getReportsCount();

	Report createReport(Report report) throws Exception;

	Report saveReport(Report report);

	Report getReportById(Long id) throws Exception;

	void resolveReport(Long id) throws Exception;

	void deleteReport(Long id) throws Exception;
}
