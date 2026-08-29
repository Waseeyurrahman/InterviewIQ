package com.interviewiq.interviewstarter.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class InterviewDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewRequest {

        @NotBlank(message = "Role is required")
        @Size(max = 100, message = "Role must not exceed 100 characters")
        private String role;

        @NotBlank(message = "Experience level is required")
        @Size(max = 50, message = "Experience level must not exceed 50 characters")
        private String experienceLevel;

        @NotBlank(message = "Difficulty is required")
        @Size(max = 50, message = "Difficulty must not exceed 50 characters")
        private String difficulty;

        @NotNull(message = "Duration is required")
        @Min(value = 5, message = "Duration must be at least 5 minutes")
        @Max(value = 180, message = "Duration must not exceed 180 minutes")
        private Integer duration;
    }

    @Data
    @AllArgsConstructor
    public static class InterviewResponse{
        private boolean success;
        private String message;
        private Long interviewId;
    }

    @Data
    @AllArgsConstructor
    public static class InterviewDetailsResponse {

        private Long id;
        private String role;
        private String experienceLevel;
        private String difficulty;
        private Integer duration;
        private String status;
        private Integer finalScore;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }


}
