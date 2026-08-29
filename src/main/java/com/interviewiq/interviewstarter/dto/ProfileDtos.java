package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProfileDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfileResponse {

        private Long id;
        private String name;
        private String email;
        private long totalInterviews;
    }
}