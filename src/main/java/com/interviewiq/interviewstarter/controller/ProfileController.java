package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.ProfileDtos;
import com.interviewiq.interviewstarter.security.CustomUserDetails;
import com.interviewiq.interviewstarter.service.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileDtos.ProfileResponse getProfile(
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long userId =
                userDetails.getUserId();

        return profileService.getProfile(userId);
    }
}