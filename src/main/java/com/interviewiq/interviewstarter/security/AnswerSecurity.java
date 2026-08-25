package com.interviewiq.interviewstarter.security;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("answerSecurity")
public class AnswerSecurity {

    private final QuestionRepository questionRepository;

    public AnswerSecurity(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public boolean isQuestionOwner(
            Long questionId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long authenticatedUserId = userDetails.getUserId();

        return questionRepository.findById(questionId)
                .map(Question::getInterview)
                .map(Interview::getUser)
                .map(user -> user.getId().equals(authenticatedUserId))
                .orElse(false);
    }
}