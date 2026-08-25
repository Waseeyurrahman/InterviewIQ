package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.InterviewDtos.*;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.security.CustomUserDetails;
import com.interviewiq.interviewstarter.service.InterviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interview")
public class Interviewcontroller {
    private final InterviewService interviewService;

    public Interviewcontroller(InterviewService interviewService){
        this.interviewService = interviewService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create")
    public InterviewResponse create(@RequestBody InterviewRequest request, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal() ;
        Long userId = userDetails.getUserId();
        Interview saved = interviewService.create(request.getRole(), request.getExperienceLevel(),request.getDifficulty(), request.getDuration(),userId);
        return new InterviewResponse(true,"Interview created",saved.getId());
    }

    @PreAuthorize("@interviewSecurity.isOwner(#id,authentication)")
    @PostMapping("/{id}/finish")
    public InterviewResponse finish(@PathVariable Long id, @RequestBody FinishInterviewRequest request){
        Interview saved = interviewService.finish(id, request.getFinalScore());
        return new InterviewResponse(true, "Interview finished", saved.getId());
    }
}
