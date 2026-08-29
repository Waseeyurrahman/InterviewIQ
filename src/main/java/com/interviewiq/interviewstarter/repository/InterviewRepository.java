package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserId(Long userId);


    // ============================================================
    // START INTERVIEW
    // CREATED → IN_PROGRESS
    // Also records startedAt
    // ============================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE Interview i
            SET i.status = :newStatus,
                i.startedAt = :startedAt
            WHERE i.id = :interviewId
              AND i.status = :currentStatus
            """)
    int startInterview(
            @Param("interviewId") Long interviewId,
            @Param("currentStatus") InterviewStatus currentStatus,
            @Param("newStatus") InterviewStatus newStatus,
            @Param("startedAt") LocalDateTime startedAt
    );



    // FINISH INTERVIEW
    // IN_PROGRESS → COMPLETED
    // Also records completedAt


    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE Interview i
            SET i.status = :newStatus,
                i.completedAt = :completedAt
            WHERE i.id = :interviewId
              AND i.status = :currentStatus
            """)
    int finishInterview(
            @Param("interviewId") Long interviewId,
            @Param("currentStatus") InterviewStatus currentStatus,
            @Param("newStatus") InterviewStatus newStatus,
            @Param("completedAt") LocalDateTime completedAt
    );



    // GENERIC STATUS TRANSITION
    // Used by EvaluationService

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query("""
            UPDATE Interview i
            SET i.status = :newStatus
            WHERE i.id = :interviewId
              AND i.status = :currentStatus
            """)
    int updateStatusIfCurrent(
            @Param("interviewId") Long interviewId,
            @Param("currentStatus") InterviewStatus currentStatus,
            @Param("newStatus") InterviewStatus newStatus
    );
}