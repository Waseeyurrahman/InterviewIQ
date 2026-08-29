package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.AnswerDtos.*;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.exception.ForbiddenException;
import com.interviewiq.interviewstarter.security.AnswerSecurity;
import com.interviewiq.interviewstarter.security.InterviewSecurity;
import com.interviewiq.interviewstarter.service.AnswerService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/answers")
public class AnswerController {

    private final AnswerService answerService;
    private final AnswerSecurity answerSecurity;
    private final InterviewSecurity interviewSecurity;

    public AnswerController(
            AnswerService answerService,
            AnswerSecurity answerSecurity,
            InterviewSecurity interviewSecurity) {

        this.answerService = answerService;
        this.answerSecurity = answerSecurity;
        this.interviewSecurity = interviewSecurity;
    }

    // Save or update one answer
    @PostMapping("/{questionId}")
    public AnswerResponse submitAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody AnswerRequest request,
            Authentication authentication) {

        if (!answerSecurity.isQuestionOwner(questionId, authentication)) {
            throw new ForbiddenException(
                    "You are not allowed to answer this question"
            );
        }

        Answer saved = answerService.save(
                questionId,
                request.getAnswerText()
        );

        return new AnswerResponse(
                true,
                "Answer saved successfully",
                saved.getId()
        );
    }

    // Get saved answers for an interview
    @GetMapping("/interview/{interviewId}")
    public List<SavedAnswerResponse> getAnswersForInterview(
            @PathVariable Long interviewId,
            Authentication authentication) {

        if (!interviewSecurity.isOwner(interviewId, authentication)) {
            throw new ForbiddenException(
                    "You are not allowed to view these answers"
            );
        }

        return answerService
                .getAnswersForInterview(interviewId)
                .stream()
                .map(answer -> new SavedAnswerResponse(
                        answer.getQuestion().getId(),
                        answer.getAnswerText()
                ))
                .toList();
    }

    public record SavedAnswerResponse(
            Long questionId,
            String answerText
    ) {}

    // Bulk endpoint kept for compatibility
    @PostMapping
    public SubmitAnswersResponse submitAnswers(
            @Valid @RequestBody SubmitAnswersRequest request,
            Authentication authentication) {

        List<Answer> answers = new ArrayList<>();

        for (SubmitAnswerItem item : request.getAnswers()) {

            if (!answerSecurity.isQuestionOwner(
                    item.getQuestionId(),
                    authentication)) {

                throw new ForbiddenException(
                        "You are not allowed to answer this question"
                );
            }

            Answer answer = new Answer();

            answer.setQuestion(
                    answerService.getQuestion(item.getQuestionId())
            );

            answer.setAnswerText(item.getAnswerText());

            answers.add(answer);
        }

        List<Answer> saved = answerService.saveAll(answers);

        return new SubmitAnswersResponse(
                true,
                saved.size()
        );
    }
}