package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                .orElseThrow(() ->  new  ResourceNotFoundException(
                        "Question not found: " + questionId
                ));
    }

    @Transactional
    public Answer save(Long questionId, String answerText) {
        Question question = getQuestion(questionId);

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setAnswerText(answerText);
        return answerRepository.save(answer);
    }

    @Transactional
    public List<Answer> saveAll(List<Answer> answers) {

        List<Answer> savedAnswers = new java.util.ArrayList<>();

        for (Answer incomingAnswer : answers) {

            Long questionId =
                    incomingAnswer.getQuestion().getId();

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

                savedAnswers.add(
                        answerRepository.save(incomingAnswer)
                );
            }
        }

        return savedAnswers;
    }
}
