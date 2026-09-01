package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionRepository questionRepository;

    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {

        evaluationService =
                new EvaluationService(
                        aiService,
                        evaluationRepository,
                        answerRepository,
                        interviewRepository,
                        questionRepository,
                        new ObjectMapper()
                );
    }


    // ============================================================
    // NO ANSWERS
    // ============================================================

    @Test
    void shouldFailWhenNoQuestionsAreAnswered() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.COMPLETED
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.COMPLETED,
                InterviewStatus.EVALUATING
        )).thenReturn(1);

        when(questionRepository.countByInterviewId(24L))
                .thenReturn(5L);

        when(answerRepository
                .findByQuestionInterviewIdOrderByIdAsc(24L))
                .thenReturn(List.of());

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(24L);

        assertFalse(result.aiAvailable);
        assertEquals(5, result.totalQuestions);
        assertEquals(0, result.answeredQuestions);
        assertEquals(5, result.skippedQuestions);
        assertEquals(InterviewStatus.FAILED, interview.getStatus());

        verify(aiService, never())
                .evaluateInterviewWithAI(anyList(), anyList());
    }


    // ============================================================
    // AI UNAVAILABLE
    // ============================================================



    @Test
    void shouldUseFallbackWhenAIIsUnavailable() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.COMPLETED
                );

        List<Answer> answers =
                createAnswers(
                        interview,
                        3
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.COMPLETED,
                InterviewStatus.EVALUATING
        )).thenReturn(1);

        when(questionRepository.countByInterviewId(24L))
                .thenReturn(5L);

        when(answerRepository
                .findByQuestionInterviewIdOrderByIdAsc(24L))
                .thenReturn(answers);

        // Gemini unavailable
        when(aiService.evaluateInterviewWithAI(
                anyList(),
                anyList()
        )).thenReturn(null);

        // Fallback evaluation for every answer
        when(aiService.evaluateWithFallback(
                anyString(),
                anyString()
        )).thenReturn(
                createEvaluation(60, 1, 70)
        );

        when(evaluationRepository.findByAnswerId(anyLong()))
                .thenReturn(Optional.empty());

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(24L);

        // AI itself was unavailable
        assertFalse(result.aiAvailable);

        assertEquals(5, result.totalQuestions);
        assertEquals(3, result.answeredQuestions);
        assertEquals(2, result.skippedQuestions);

        // Fallback evaluations were used
        assertEquals(60, result.score);
        assertEquals(70, result.confidence);
        assertEquals(3, result.fillerWords);

        // Interview should still be successfully evaluated
        assertEquals(
                InterviewStatus.EVALUATED,
                interview.getStatus()
        );

        assertEquals(
                60,
                interview.getFinalScore()
        );

        // Fallback called once for each answered question
        verify(aiService, times(3))
                .evaluateWithFallback(
                        anyString(),
                        anyString()
                );

        // Three evaluation records should be saved
        verify(evaluationRepository, times(3))
                .save(any(Evaluation.class));
    }


    // ============================================================
    // WRONG NUMBER OF AI EVALUATIONS
    // ============================================================

    @Test
    void shouldUseFallbackWhenAIReturnsWrongNumberOfEvaluations() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.COMPLETED
                );

        List<Answer> answers =
                createAnswers(
                        interview,
                        3
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.COMPLETED,
                InterviewStatus.EVALUATING
        )).thenReturn(1);

        when(questionRepository.countByInterviewId(24L))
                .thenReturn(5L);

        when(answerRepository
                .findByQuestionInterviewIdOrderByIdAsc(24L))
                .thenReturn(answers);

        // Gemini incorrectly returns only 2 evaluations
        // when 3 are required.
        when(aiService.evaluateInterviewWithAI(
                anyList(),
                anyList()
        )).thenReturn(
                List.of(
                        createEvaluation(),
                        createEvaluation()
                )
        );

        // Fallback handles the failure
        when(aiService.evaluateWithFallback(
                anyString(),
                anyString()
        )).thenReturn(
                createEvaluation(60, 1, 70)
        );

        when(evaluationRepository.findByAnswerId(anyLong()))
                .thenReturn(Optional.empty());

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(24L);

        // Gemini was not successfully used
        assertFalse(result.aiAvailable);

        // Fallback was used
        assertEquals(60, result.score);
        assertEquals(70, result.confidence);
        assertEquals(3, result.fillerWords);

        // Interview should still be evaluated
        assertEquals(
                InterviewStatus.EVALUATED,
                interview.getStatus()
        );

        assertEquals(
                60,
                interview.getFinalScore()
        );

        // Fallback once per answer
        verify(aiService, times(3))
                .evaluateWithFallback(
                        anyString(),
                        anyString()
                );

        // All three answers receive evaluations
        verify(evaluationRepository, times(3))
                .save(any(Evaluation.class));
    }


    // ============================================================
    // SUCCESSFUL EVALUATION
    // ============================================================

    @Test
    void shouldEvaluateInterviewSuccessfully() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.COMPLETED
                );

        List<Answer> answers =
                createAnswers(
                        interview,
                        3
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.COMPLETED,
                InterviewStatus.EVALUATING
        )).thenReturn(1);

        when(questionRepository.countByInterviewId(24L))
                .thenReturn(5L);

        when(answerRepository
                .findByQuestionInterviewIdOrderByIdAsc(24L))
                .thenReturn(answers);

        when(aiService.evaluateInterviewWithAI(
                anyList(),
                anyList()
        )).thenReturn(
                List.of(
                        createEvaluation(80, 10, 70),
                        createEvaluation(60, 5, 80),
                        createEvaluation(90, 2, 90)
                )
        );

        when(evaluationRepository.findByAnswerId(anyLong()))
                .thenReturn(Optional.empty());

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(24L);

        assertTrue(result.aiAvailable);

        assertEquals(5, result.totalQuestions);
        assertEquals(3, result.answeredQuestions);
        assertEquals(2, result.skippedQuestions);

        assertEquals(
                76,
                result.score
        );

        assertEquals(
                80,
                result.confidence
        );

        assertEquals(
                17,
                result.fillerWords
        );

        assertEquals(
                InterviewStatus.EVALUATED,
                interview.getStatus()
        );

        assertEquals(
                76,
                interview.getFinalScore()
        );

        verify(evaluationRepository, times(3))
                .save(any(Evaluation.class));
    }


    // ============================================================
    // FAILED INTERVIEW CAN BE RETRIED
    // ============================================================

    @Test
    void shouldAllowRetryAfterPreviousFailure() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.FAILED
                );

        List<Answer> answers =
                createAnswers(
                        interview,
                        2
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.FAILED,
                InterviewStatus.EVALUATING
        )).thenReturn(1);

        when(questionRepository.countByInterviewId(24L))
                .thenReturn(2L);

        when(answerRepository
                .findByQuestionInterviewIdOrderByIdAsc(24L))
                .thenReturn(answers);

        when(aiService.evaluateInterviewWithAI(
                anyList(),
                anyList()
        )).thenReturn(
                List.of(
                        createEvaluation(),
                        createEvaluation()
                )
        );

        when(evaluationRepository.findByAnswerId(anyLong()))
                .thenReturn(Optional.empty());

        EvaluationService.OverallResult result =
                evaluationService.evaluateInterview(24L);

        assertTrue(result.aiAvailable);

        assertEquals(
                InterviewStatus.EVALUATED,
                interview.getStatus()
        );
    }


    // ============================================================
    // ALREADY EVALUATED
    // ============================================================

    @Test
    void shouldRejectAlreadyEvaluatedInterview() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.EVALUATED
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        assertThrows(
                IllegalStateException.class,
                () -> evaluationService.evaluateInterview(24L)
        );

        verifyNoInteractions(aiService);
    }


    // ============================================================
    // CONCURRENT EVALUATION PROTECTION
    // ============================================================

    @Test
    void shouldRejectWhenAnotherEvaluationAlreadyClaimedInterview() {

        Interview interview =
                createInterview(
                        24L,
                        InterviewStatus.COMPLETED
                );

        when(interviewRepository.findById(24L))
                .thenReturn(Optional.of(interview));

        when(interviewRepository.updateStatusIfCurrent(
                24L,
                InterviewStatus.COMPLETED,
                InterviewStatus.EVALUATING
        )).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> evaluationService.evaluateInterview(24L)
        );

        verifyNoInteractions(aiService);
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private Interview createInterview(
            Long id,
            InterviewStatus status) {

        Interview interview =
                new Interview();

        interview.setId(id);
        interview.setRole("Java Developer");
        interview.setExperienceLevel("Mid");
        interview.setDifficulty("Medium");
        interview.setDuration(30);
        interview.setStatus(status);

        return interview;
    }


    private List<Answer> createAnswers(
            Interview interview,
            int count) {

        List<Answer> answers =
                new java.util.ArrayList<>();

        for (int i = 1; i <= count; i++) {

            Question question =
                    new Question();

            question.setId((long) i);
            question.setInterview(interview);
            question.setQuestionText(
                    "Question " + i
            );

            Answer answer =
                    new Answer();

            answer.setId((long) i);
            answer.setQuestion(question);
            answer.setAnswerText(
                    "Candidate answer " + i
            );

            answers.add(answer);
        }

        return answers;
    }


    private AIService.AIEvaluation createEvaluation() {

        return createEvaluation(
                80,
                5,
                75
        );
    }


    private AIService.AIEvaluation createEvaluation(
            int score,
            int fillerWords,
            int confidence) {

        AIService.AIEvaluation evaluation =
                new AIService.AIEvaluation();

        evaluation.score = score;
        evaluation.fillerWords = fillerWords;
        evaluation.confidence = confidence;
        evaluation.relevance = "high";
        evaluation.technicalAccuracy = "good";

        evaluation.strengths =
                List.of("Clear explanation");

        evaluation.weaknesses =
                List.of("Could be more concise");

        evaluation.recommendations =
                List.of("Add more technical detail");

        return evaluation;
    }
}