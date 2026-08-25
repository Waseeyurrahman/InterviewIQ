package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository
        extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByAnswerId(Long answerId);
    List<Evaluation> findByAnswerQuestionInterviewIdOrderByIdAsc(
            Long interviewId);
    List<Evaluation> findByAnswerQuestionInterviewUserId(Long userId);
}