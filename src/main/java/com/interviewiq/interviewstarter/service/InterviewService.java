package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public InterviewService(
            InterviewRepository interviewRepository,
            UserRepository userRepository) {

        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    // ============================================================
    // CREATE INTERVIEW
    // ============================================================

    public Interview create(
            String role,
            String experienceLevel,
            String difficulty,
            Integer duration,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        ));

        Interview interview = new Interview();

        interview.setRole(role);
        interview.setExperienceLevel(experienceLevel);
        interview.setDifficulty(difficulty);
        interview.setDuration(duration);

        // Associate interview with logged-in user
        interview.setUser(user);

        // Initial lifecycle state
        interview.setStatus(InterviewStatus.CREATED);

        return interviewRepository.save(interview);
    }


    // ============================================================
    // START INTERVIEW
    // CREATED → IN_PROGRESS
    // ============================================================

    @Transactional
    public Interview startInterview(Long interviewId) {

        Interview interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview not found: " + interviewId
                        ));

        int updated = interviewRepository.updateStatusIfCurrent(
                interviewId,
                InterviewStatus.CREATED,
                InterviewStatus.IN_PROGRESS
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Interview cannot be started from status "
                            + interview.getStatus()
            );
        }

        interview.setStatus(InterviewStatus.IN_PROGRESS);

        return interview;
    }


    @Transactional
    public Interview finish(Long interviewId) {

        Interview interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview not found: " + interviewId
                        ));

        int updated = interviewRepository.updateStatusIfCurrent(
                interviewId,
                InterviewStatus.IN_PROGRESS,
                InterviewStatus.COMPLETED
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Interview cannot be finished from status "
                            + interview.getStatus()
            );
        }

        interview.setCompletedAt(
                LocalDateTime.now()
        );

        interview.setStatus(
                InterviewStatus.COMPLETED
        );

        return interview;
    }
}