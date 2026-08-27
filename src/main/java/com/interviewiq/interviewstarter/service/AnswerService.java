package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.exception.InvalidRequestException;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnswerService {
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public AnswerService(AnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    public Question getQuestion(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question not found: " + questionId
                ));
    }

    private void validateInterviewInProgress(Question question) {

        if (question.getInterview().getStatus()
                != InterviewStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "Answers can only be submitted while the interview is in progress"
            );
        }
    }

    @Transactional
    public Answer save(Long questionId, String answerText) {
        Question question = getQuestion(questionId);

        validateInterviewInProgress(question);

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setAnswerText(answerText);
        return answerRepository.save(answer);
    }

    @Transactional
    public List<Answer> saveAll(List<Answer> answers) {

        Set<Long> questionIds = new HashSet<>();
        List<Answer> savedAnswers = new java.util.ArrayList<>();

        for (Answer incomingAnswer : answers) {

            Long questionId =
                    incomingAnswer.getQuestion().getId();

            // Reject duplicate questions in the same request
            if (!questionIds.add(questionId)) {
                throw new InvalidRequestException(
                        "Duplicate question ID in request: " + questionId
                );
            }

            Question question =
                    getQuestion(questionId);

            if (question.getInterview().getStatus()
                    != InterviewStatus.IN_PROGRESS) {

                throw new IllegalStateException(
                        "Answers can only be submitted while the interview is in progress"
                );
            }

            Answer existingAnswer =
                    answerRepository
                            .findByQuestionId(questionId)
                            .orElse(null);

            if (existingAnswer != null) {

                existingAnswer.setAnswerText(
                        incomingAnswer.getAnswerText()
                );

                savedAnswers.add(
                        answerRepository.save(existingAnswer)
                );

            } else {

                incomingAnswer.setQuestion(question);

                savedAnswers.add(
                        answerRepository.save(incomingAnswer)
                );
            }
        }

        return savedAnswers;
    }
}