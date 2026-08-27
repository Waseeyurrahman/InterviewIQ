package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByUserId(Long userId);

    @Modifying
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