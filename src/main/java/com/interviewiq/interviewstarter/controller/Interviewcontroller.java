package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.InterviewDtos.*;
import com.interviewiq.interviewstarter.dto.MyInterviewResponse;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.security.CustomUserDetails;
import com.interviewiq.interviewstarter.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interview")
public class Interviewcontroller {
    private final InterviewService interviewService;

    public Interviewcontroller(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create")
    public InterviewResponse create(@Valid @RequestBody InterviewRequest request, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        Interview saved = interviewService.create(request.getRole(), request.getExperienceLevel(), request.getDifficulty(), request.getDuration(), userId);
        return new InterviewResponse(true, "Interview created", saved.getId());
    }

    @PreAuthorize("@interviewSecurity.isOwner(#id,authentication)")
    @PostMapping("/{id}/start")
    public InterviewResponse start(
            @PathVariable Long id) {

        Interview started =
                interviewService.startInterview(id);

        return new InterviewResponse(
                true,
                "Interview started",
                started.getId()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@interviewSecurity.isOwner(#id, authentication)")
    public InterviewDetailsResponse getInterview(
            @PathVariable Long id) {

        Interview interview =
                interviewService.getInterview(id);

        return new InterviewDetailsResponse(
                interview.getId(),
                interview.getRole(),
                interview.getExperienceLevel(),
                interview.getDifficulty(),
                interview.getDuration(),
                interview.getStatus().name(),
                interview.getFinalScore(),
                interview.getStartedAt(),
                interview.getCompletedAt()
        );
    }

    @PreAuthorize("@interviewSecurity.isOwner(#id,authentication)")
    @PostMapping("/{id}/finish")
    public InterviewResponse finish(
            @PathVariable Long id) {

        Interview saved =
                interviewService.finish(id);

        return new InterviewResponse(
                true,
                "Interview finished",
                saved.getId()
        );
    }
    @GetMapping("/my-interviews")
    @PreAuthorize("hasRole('USER')")
    public List<MyInterviewResponse> getMyInterviews(
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId =
                userDetails.getUserId();

        return interviewService
                .getInterviewsForUser(userId)
                .stream()
                .map(interview ->
                        new MyInterviewResponse(
                                interview.getId(),
                                interview.getRole(),
                                interview.getExperienceLevel(),
                                interview.getDifficulty(),
                                interview.getDuration(),
                                interview.getFinalScore(),
                                interview.getStatus().name(),
                                interview.getStartedAt(),
                                interview.getCompletedAt()
                        )
                )
                .toList();
    }
}
