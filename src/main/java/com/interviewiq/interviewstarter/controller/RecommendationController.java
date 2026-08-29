package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.RecommendationDtos;
import com.interviewiq.interviewstarter.security.CustomUserDetails;
import com.interviewiq.interviewstarter.service.RecommendationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService) {

        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationDtos.RecommendationResponse
    getRecommendations(Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId = userDetails.getUserId();

        return recommendationService
                .getRecommendations(userId);
    }
}