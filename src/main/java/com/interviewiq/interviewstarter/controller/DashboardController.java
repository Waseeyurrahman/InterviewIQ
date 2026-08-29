package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.security.CustomUserDetails;
import com.interviewiq.interviewstarter.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse getDashboard(Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId = userDetails.getUserId();

        return dashboardService.buildDashboard(userId);
    }
}