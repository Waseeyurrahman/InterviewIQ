package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewService {

    private static final Logger log =
            LoggerFactory.getLogger(InterviewService.class);

    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    public InterviewService(
            InterviewRepository interviewRepository,
            UserRepository userRepository) {

        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }



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

        Interview saved =
                interviewRepository.save(interview);

        log.info(
                "Interview {} created for user {}",
                saved.getId(),
                userId
        );

        return saved;
    }



    @Transactional
    public Interview startInterview(Long interviewId) {

        Interview interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview not found: " + interviewId
                        ));

        LocalDateTime startedAt = LocalDateTime.now();

        int updated = interviewRepository.startInterview(
                interviewId,
                InterviewStatus.CREATED,
                InterviewStatus.IN_PROGRESS,
                startedAt
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Interview cannot be started from status "
                            + interview.getStatus()
            );
        }

        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartedAt(startedAt);

        log.info(
                "Interview {} starte at {}",
                interviewId,
                startedAt
        );

        return interview;
    }

    @Transactional(readOnly = true)
    public Interview getInterview(Long interviewId) {

        return interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview not found: " + interviewId
                        ));
    }


    @Transactional
    public Interview finish(Long interviewId) {

        Interview interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Interview not found: " + interviewId
                        ));

        LocalDateTime completedAt = LocalDateTime.now();

        int updated = interviewRepository.finishInterview(
                interviewId,
                InterviewStatus.IN_PROGRESS,
                InterviewStatus.COMPLETED,
                completedAt
        );

        if (updated == 0) {
            throw new IllegalStateException(
                    "Interview cannot be finished from status "
                            + interview.getStatus()
            );
        }

        interview.setStatus(
                InterviewStatus.COMPLETED
        );

        interview.setCompletedAt(
                completedAt
        );


        log.info(
                "Interview {} finished at {}",
                interviewId,
                completedAt
        );

        return interview;
    }

    @Transactional(readOnly = true)
    public List<Interview> getInterviewsForUser(Long userId) {

        return interviewRepository.findByUserId(userId);
    }
}