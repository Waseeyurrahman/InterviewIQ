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

        System.out.println("=== INTERVIEW SECURITY ===");
        System.out.println("Interview ID: " + interviewId);
        System.out.println("Authentication: " + authentication);
        System.out.println("Principal: " + authentication.getPrincipal());
        System.out.println("Principal class: "
                + authentication.getPrincipal().getClass());

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long authenticatedUserId = userDetails.getUserId();

        System.out.println("Authenticated User ID: " + authenticatedUserId);

        return interviewRepository.findById(interviewId)
                .map(interview -> {

                    System.out.println("Interview found: " + interview.getId());
                    System.out.println("Interview User: " + interview.getUser());

                    Long interviewUserId = interview.getUser().getId();

                    System.out.println("Interview User ID: " + interviewUserId);

                    return interviewUserId.equals(authenticatedUserId);
                })
                .orElse(false);
    }
}