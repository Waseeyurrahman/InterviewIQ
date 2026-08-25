package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository  extends JpaRepository<Interview,Long> {
    List<Interview> findByUserId(Long userId);
}
