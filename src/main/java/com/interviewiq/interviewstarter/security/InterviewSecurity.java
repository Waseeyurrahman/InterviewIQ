package com.interviewiq.interviewstarter.security;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("interviewSecurity")
public class InterviewSecurity {

    private final InterviewRepository interviewRepository;

    public InterviewSecurity(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    public boolean isOwner(Long interviewId, Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long authenticatedUserId =
                userDetails.getUserId();

        return interviewRepository.findById(interviewId)
                .map(interview ->
                        interview.getUser().getId()
                                .equals(authenticatedUserId)
                )
                .orElse(false);
    }
}