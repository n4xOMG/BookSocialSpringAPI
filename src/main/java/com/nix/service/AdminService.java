package com.nix.service;

import com.nix.dtos.ContentAnalyticsDTO;
import com.nix.dtos.PlatformAnalyticsDTO;
import com.nix.dtos.RevenueAnalyticsDTO;
import com.nix.dtos.UserAnalyticsDTO;

public interface AdminService {

	UserAnalyticsDTO getUserAnalytics();

	RevenueAnalyticsDTO getRevenueAnalytics();

	ContentAnalyticsDTO getContentAnalytics();

	PlatformAnalyticsDTO getPlatformAnalytics();
}
