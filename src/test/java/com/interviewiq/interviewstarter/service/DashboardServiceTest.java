package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                interviewRepository,
                evaluationRepository,
                new ObjectMapper()
        );
    }

    @Test
    void buildDashboard_shouldOnlyUseInterviewsForRequestedUser() {

        Interview userInterview = interview(
                1L,
                "Java Developer",
                80,
                30
        );

        when(interviewRepository.findByUserId(1L))
                .thenReturn(List.of(userInterview));

        when(evaluationRepository
                .findByAnswerQuestionInterviewUserId(1L))
                .thenReturn(List.of());

        DashboardResponse result =
                dashboardService.buildDashboard(1L);

        assertEquals(1, result.getTotalInterviews());
        assertEquals(80, result.getAverageScore());
        assertEquals(80, result.getBestScore());
        assertEquals("30m", result.getTotalPracticeTime());

        verify(interviewRepository)
                .findByUserId(1L);

        verify(interviewRepository, never())
                .findByUserId(2L);
    }

    @Test
    void buildDashboard_shouldIgnoreInterviewsWithoutFinalScore() {

        Interview evaluated = interview(
                1L,
                "Java Developer",
                80,
                30
        );

        Interview notEvaluated = interview(
                2L,
                "Spring Boot Developer",
                null,
                45
        );

        when(interviewRepository.findByUserId(1L))
                .thenReturn(List.of(
                        evaluated,
                        notEvaluated
                ));

        when(evaluationRepository
                .findByAnswerQuestionInterviewUserId(1L))
                .thenReturn(List.of());

        DashboardResponse result =
                dashboardService.buildDashboard(1L);

        assertEquals(1, result.getTotalInterviews());
        assertEquals(80, result.getAverageScore());
        assertEquals(80, result.getBestScore());
        assertEquals("30m", result.getTotalPracticeTime());
    }

    @Test
    void buildDashboard_shouldCalculateAverageBestAndPracticeTime() {

        Interview first = interview(
                1L,
                "Java Developer",
                80,
                30
        );

        Interview second = interview(
                2L,
                "Spring Boot Developer",
                60,
                90
        );

        when(interviewRepository.findByUserId(1L))
                .thenReturn(List.of(first, second));

        when(evaluationRepository
                .findByAnswerQuestionInterviewUserId(1L))
                .thenReturn(List.of());

        DashboardResponse result =
                dashboardService.buildDashboard(1L);

        assertEquals(2, result.getTotalInterviews());
        assertEquals(70, result.getAverageScore());
        assertEquals(80, result.getBestScore());
        assertEquals("2h0m", result.getTotalPracticeTime());
    }

    private Interview interview(
            Long id,
            String role,
            Integer finalScore,
            Integer duration) {

        Interview interview = new Interview();

        interview.setId(id);
        interview.setRole(role);
        interview.setFinalScore(finalScore);
        interview.setDuration(duration);

        return interview;
    }
}