package com.interviewiq.interviewstarter.controller;


import com.interviewiq.interviewstarter.dto.AnswerDtos.*;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.exception.ForbiddenException;
import com.interviewiq.interviewstarter.security.AnswerSecurity;
import com.interviewiq.interviewstarter.service.AnswerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/answers")
public class AnswerController {

    private final AnswerService answerService;
    private final AnswerSecurity answerSecurity;

    public AnswerController(AnswerService answerService,AnswerSecurity answerSecurity){
        this.answerService = answerService;
        this.answerSecurity = answerSecurity;
    }

    @PostMapping
    public SubmitAnswersResponse submitAnswers(@RequestBody SubmitAnswersRequest request, Authentication authentication){
        List<Answer> toSave = new ArrayList<>();
        if (request.getAnswers() != null) {
            for (SubmitAnswerItem item : request.getAnswers()) {
                // Verify that the question belongs
                // to the authenticated user's interview
                if (!answerSecurity.isQuestionOwner(
                        item.getQuestionId(),
                        authentication)) {

                    throw new ForbiddenException(
                            "You are not allowed to answer this question"
                    );
                }
                Answer a = new Answer();
                a.setQuestion(answerService.getQuestion(item.getQuestionId()));
                a.setAnswerText(item.getAnswerText());
                toSave.add(a);
            }
        }
        List<Answer> saved = answerService.saveAll(toSave);
        return new SubmitAnswersResponse(true, saved.size());
    }

}
