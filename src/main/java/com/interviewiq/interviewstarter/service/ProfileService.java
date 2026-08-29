package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.dto.ProfileDtos;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProfileDtos.ProfileResponse getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        ));

        long totalInterviews =
                user.getInterviews() == null
                        ? 0
                        : user.getInterviews().size();

        return ProfileDtos.ProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .totalInterviews(totalInterviews)
                .build();
    }
}