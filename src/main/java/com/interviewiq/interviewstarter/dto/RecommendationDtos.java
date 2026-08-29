package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class RecommendationDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationResponse {

        private List<RecommendationItem> recommendations;
        private List<RecommendationItem> weakAreas;
        private List<RecommendationItem> strengths;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationItem {

        private String text;
        private int count;
    }
}