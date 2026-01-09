package com.nix.service;

import java.util.List;

import com.nix.dtos.ActiveUserAnalyticsDTO;
import com.nix.dtos.BestBookAnalyticsDTO;
import com.nix.dtos.ContentAnalyticsDTO;
import com.nix.dtos.PlatformAnalyticsDTO;
import com.nix.dtos.RevenueAnalyticsDTO;
import com.nix.dtos.TopSpenderAnalyticsDTO;
import com.nix.dtos.UserAnalyticsDTO;

public interface AdminService {

	UserAnalyticsDTO getUserAnalytics();

	RevenueAnalyticsDTO getRevenueAnalytics();

	ContentAnalyticsDTO getContentAnalytics();

	PlatformAnalyticsDTO getPlatformAnalytics();

	List<BestBookAnalyticsDTO> getBestBooks(String period, int limit);

	List<ActiveUserAnalyticsDTO> getMostActiveUsers(String period, int limit);

	List<TopSpenderAnalyticsDTO> getTopSpenders(String period, int limit);
}
