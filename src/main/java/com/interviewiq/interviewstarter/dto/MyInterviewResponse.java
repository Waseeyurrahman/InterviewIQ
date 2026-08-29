package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyInterviewResponse {

    private Long interviewId;
    private String role;
    private String experienceLevel;
    private String difficulty;
    private Integer duration;
    private Integer score;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}