package com.interviewiq.interviewstarter.repository;

import com.interviewiq.interviewstarter.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQuestionInterviewIdOrderByIdAsc(Long interviewId);
    Optional<Answer> findByQuestionId(Long questionId);
}