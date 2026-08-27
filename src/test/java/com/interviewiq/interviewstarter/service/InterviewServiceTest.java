package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InterviewService interviewService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");
    }

    @Test
    void create_shouldCreateInterviewInCreatedState() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Interview saved = new Interview();
        saved.setId(10L);
        saved.setUser(user);
        saved.setStatus(InterviewStatus.CREATED);

        when(interviewRepository.save(any(Interview.class)))
                .thenReturn(saved);

        Interview result = interviewService.create(
                "Java Developer",
                "Mid Level",
                "Medium",
                30,
                1L
        );

        assertEquals(10L, result.getId());
        assertEquals(user, result.getUser());
        assertEquals(
                InterviewStatus.CREATED,
                result.getStatus()
        );

        verify(userRepository).findById(1L);
        verify(interviewRepository).save(any(Interview.class));
    }

    @Test
    void create_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> interviewService.create(
                        "Java Developer",
                        "Mid Level",
                        "Medium",
                        30,
                        1L
                )
        );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }

    @Test
    void startInterview_shouldMoveCreatedToInProgress() {

        Interview interview = interview(
                10L,
                InterviewStatus.CREATED
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                10L,
                InterviewStatus.CREATED,
                InterviewStatus.IN_PROGRESS
        )).thenReturn(1);

        Interview result =
                interviewService.startInterview(10L);

        assertEquals(
                InterviewStatus.IN_PROGRESS,
                result.getStatus()
        );

        verify(interviewRepository)
                .updateStatusIfCurrent(
                        10L,
                        InterviewStatus.CREATED,
                        InterviewStatus.IN_PROGRESS
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }

    @Test
    void startInterview_shouldRejectNonCreatedInterview() {

        Interview interview = interview(
                10L,
                InterviewStatus.IN_PROGRESS
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                10L,
                InterviewStatus.CREATED,
                InterviewStatus.IN_PROGRESS
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.startInterview(10L)
        );

        verify(interviewRepository)
                .updateStatusIfCurrent(
                        10L,
                        InterviewStatus.CREATED,
                        InterviewStatus.IN_PROGRESS
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }

    @Test
    void finish_shouldMoveInProgressToCompleted() {

        Interview interview = interview(
                10L,
                InterviewStatus.IN_PROGRESS
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                10L,
                InterviewStatus.IN_PROGRESS,
                InterviewStatus.COMPLETED
        )).thenReturn(1);

        Interview result =
                interviewService.finish(10L);

        assertEquals(
                InterviewStatus.COMPLETED,
                result.getStatus()
        );

        assertNotNull(result.getCompletedAt());

        verify(interviewRepository)
                .updateStatusIfCurrent(
                        10L,
                        InterviewStatus.IN_PROGRESS,
                        InterviewStatus.COMPLETED
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }

    @Test
    void finish_shouldRejectNonInProgressInterview() {

        Interview interview = interview(
                10L,
                InterviewStatus.CREATED
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                10L,
                InterviewStatus.IN_PROGRESS,
                InterviewStatus.COMPLETED
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.finish(10L)
        );

        verify(interviewRepository)
                .updateStatusIfCurrent(
                        10L,
                        InterviewStatus.IN_PROGRESS,
                        InterviewStatus.COMPLETED
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }

    @Test
    void startInterview_shouldThrowWhenInterviewDoesNotExist() {

        when(interviewRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> interviewService.startInterview(999L)
        );

        verify(interviewRepository, never())
                .updateStatusIfCurrent(
                        anyLong(),
                        any(InterviewStatus.class),
                        any(InterviewStatus.class)
                );
    }

    private Interview interview(
            Long id,
            InterviewStatus status) {

        Interview interview = new Interview();

        interview.setId(id);
        interview.setUser(user);
        interview.setStatus(status);

        return interview;
    }
}