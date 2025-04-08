package com.nix.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nix.models.Report;
import com.nix.repository.ReportRepository;

@Service
public class ReportServiceImpl implements ReportService {

	@Autowired
	private ReportRepository reportRepository;

	@Override
	public Report createReport(Report report) throws Exception {
		// Additional validations can be added here
		return reportRepository.save(report);
	}

	@Override
	public List<Report> getAllReports() {
		return reportRepository.findAll();
	}

	@Override
	public Long getReportsCount() {
		return reportRepository.count();
	}

	@Override
	public Report getReportById(Long id) throws Exception {
		Optional<Report> optionalReport = reportRepository.findById(id);
		if (optionalReport.isPresent()) {
			return optionalReport.get();
		}
		throw new Exception("Report not found with id: " + id);
	}

	@Override
	public void resolveReport(Long id) throws Exception {
		Report report = getReportById(id);
		report.setResolved(true);
		reportRepository.save(report);
	}

	@Override
	public void deleteReport(Long id) throws Exception {
		Report report = getReportById(id);
		reportRepository.delete(report);
	}

	@Override
	public Report saveReport(Report report) {
		return reportRepository.save(report);
	}

}
