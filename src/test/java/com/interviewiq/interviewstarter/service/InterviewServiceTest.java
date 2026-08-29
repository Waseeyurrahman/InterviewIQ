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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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


    // ============================================================
    // CREATE
    // ============================================================

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


    // ============================================================
    // START INTERVIEW
    // ============================================================

    @Test
    void startInterview_shouldMoveCreatedToInProgress() {

        Interview interview = interview(
                10L,
                InterviewStatus.CREATED
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.startInterview(
                eq(10L),
                eq(InterviewStatus.CREATED),
                eq(InterviewStatus.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(1);

        Interview result =
                interviewService.startInterview(10L);

        assertEquals(
                InterviewStatus.IN_PROGRESS,
                result.getStatus()
        );

        assertNotNull(
                result.getStartedAt()
        );

        verify(interviewRepository)
                .startInterview(
                        eq(10L),
                        eq(InterviewStatus.CREATED),
                        eq(InterviewStatus.IN_PROGRESS),
                        any(LocalDateTime.class)
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

        when(interviewRepository.startInterview(
                eq(10L),
                eq(InterviewStatus.CREATED),
                eq(InterviewStatus.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.startInterview(10L)
        );

        verify(interviewRepository)
                .startInterview(
                        eq(10L),
                        eq(InterviewStatus.CREATED),
                        eq(InterviewStatus.IN_PROGRESS),
                        any(LocalDateTime.class)
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }


    // ============================================================
    // FINISH INTERVIEW
    // ============================================================

    @Test
    void finish_shouldMoveInProgressToCompleted() {

        Interview interview = interview(
                10L,
                InterviewStatus.IN_PROGRESS
        );

        when(interviewRepository.findById(10L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.finishInterview(
                eq(10L),
                eq(InterviewStatus.IN_PROGRESS),
                eq(InterviewStatus.COMPLETED),
                any(LocalDateTime.class)
        )).thenReturn(1);

        Interview result =
                interviewService.finish(10L);

        assertEquals(
                InterviewStatus.COMPLETED,
                result.getStatus()
        );

        assertNotNull(
                result.getCompletedAt()
        );

        verify(interviewRepository)
                .finishInterview(
                        eq(10L),
                        eq(InterviewStatus.IN_PROGRESS),
                        eq(InterviewStatus.COMPLETED),
                        any(LocalDateTime.class)
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

        when(interviewRepository.finishInterview(
                eq(10L),
                eq(InterviewStatus.IN_PROGRESS),
                eq(InterviewStatus.COMPLETED),
                any(LocalDateTime.class)
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.finish(10L)
        );

        verify(interviewRepository)
                .finishInterview(
                        eq(10L),
                        eq(InterviewStatus.IN_PROGRESS),
                        eq(InterviewStatus.COMPLETED),
                        any(LocalDateTime.class)
                );

        verify(interviewRepository, never())
                .save(any(Interview.class));
    }


    // ============================================================
    // NOT FOUND
    // ============================================================

    @Test
    void startInterview_shouldThrowWhenInterviewDoesNotExist() {

        when(interviewRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> interviewService.startInterview(999L)
        );

        verify(interviewRepository, never())
                .startInterview(
                        anyLong(),
                        any(InterviewStatus.class),
                        any(InterviewStatus.class),
                        any(LocalDateTime.class)
                );
    }


    // ============================================================
    // HELPER
    // ============================================================

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