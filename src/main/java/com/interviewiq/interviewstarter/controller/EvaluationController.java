package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.Evaluationsdtos;
import com.interviewiq.interviewstarter.exception.ForbiddenException;
import com.interviewiq.interviewstarter.security.InterviewSecurity;
import com.interviewiq.interviewstarter.service.EvaluationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interviews")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final InterviewSecurity interviewSecurity;

    public EvaluationController(
            EvaluationService evaluationService,
            InterviewSecurity interviewSecurity) {

        this.evaluationService = evaluationService;
        this.interviewSecurity = interviewSecurity;
    }

    @PostMapping("/{interviewId}/evaluate")
    public Evaluationsdtos.EvaluateAnswersResponse evaluate(
            @PathVariable Long interviewId,
            Authentication authentication) {

        // Verify that the authenticated user owns this interview
        if (!interviewSecurity.isOwner(
                interviewId,
                authentication)) {

            throw new ForbiddenException(
                    "You are not allowed to evaluate this interview"
            );
        }

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(interviewId);

        return new Evaluationsdtos.EvaluateAnswersResponse(
                true,
                result.aiAvailable,
                result.score,
                result.fillerWords,
                result.confidence,
                result.totalQuestions,
                result.answeredQuestions,
                result.skippedQuestions,
                result.relevance,
                result.strengths,
                result.weaknesses,
                result.recommendations
        );
    }
    @GetMapping("/{interviewId}/evaluation")
    public List<Evaluationsdtos.EvaluationItemResponse>
    getEvaluation(
            @PathVariable Long interviewId,
            Authentication authentication) {

        if (!interviewSecurity.isOwner(
                interviewId,
                authentication)) {

            throw new ForbiddenException(
                    "You are not allowed to view this evaluation"
            );
        }

        return evaluationService
                .getInterviewEvaluationDetails(interviewId);
    }
}