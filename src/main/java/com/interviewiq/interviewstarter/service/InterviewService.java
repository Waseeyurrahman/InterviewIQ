package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.exception.ForbiddenException;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;


    public InterviewService(InterviewRepository interviewRepository,UserRepository userRepository){
        this.interviewRepository=interviewRepository;
        this.userRepository = userRepository;
    }

    public Interview create(String role, String experienceLevel, String difficulty, Integer duration, Long userid){
        // Find the authenticated user
        User user = userRepository.findById(userid)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
        Interview interview = new Interview();
        interview.setRole(role);
        interview.setExperienceLevel(experienceLevel);
        interview.setDifficulty(difficulty);
        interview.setDuration(duration);
        // Associate interview with logged-in user
        interview.setUser(user);
        return interviewRepository.save(interview);
    }

    public Interview finish(Long id, Integer finalScore){
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview not found: " + id
                ));


        interview.setFinalScore(finalScore);
        interview.setCompletedAt(LocalDateTime.now());
        return interviewRepository.save(interview);
    }
}
