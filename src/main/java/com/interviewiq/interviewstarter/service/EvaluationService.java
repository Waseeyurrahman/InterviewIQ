package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.dto.Evaluationsdtos;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.exception.ResourceNotFoundException;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {

    private static final Logger log =
            LoggerFactory.getLogger(EvaluationService.class);

    private final AIService aiService;
    private final EvaluationRepository evaluationRepository;
    private final AnswerRepository answerRepository;
    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            AIService aiService,
            EvaluationRepository evaluationRepository,
            AnswerRepository answerRepository,
            InterviewRepository interviewRepository,
            QuestionRepository questionRepository,
            ObjectMapper objectMapper) {

        this.aiService = aiService;
        this.evaluationRepository = evaluationRepository;
        this.answerRepository = answerRepository;
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
    }

    public static class OverallResult {

        public boolean aiAvailable = false;

        public int score = 0;
        public int fillerWords = 0;
        public int confidence = 0;

        public int totalQuestions = 0;
        public int answeredQuestions = 0;
        public int skippedQuestions = 0;

        public String relevance = "medium";

        public List<String> strengths = new ArrayList<>();
        public List<String> weaknesses = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
    }

    @Transactional
    public OverallResult evaluateInterview(Long interviewId) {

        Interview interview =
                interviewRepository.findById(interviewId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Interview not found: " + interviewId
                                )
                        );

        if (interview.getStatus() != InterviewStatus.COMPLETED
                && interview.getStatus() != InterviewStatus.FAILED) {

            throw new IllegalStateException(
                    "Interview cannot be evaluated from status "
                            + interview.getStatus()
            );
        }

        int claimed =
                interviewRepository.updateStatusIfCurrent(
                        interviewId,
                        interview.getStatus(),
                        InterviewStatus.EVALUATING
                );

        if (claimed != 1) {
            throw new IllegalStateException(
                    "Interview is already being evaluated or has already been evaluated"
            );
        }

        log.info(
                "Starting evaluation for interview {}",
                interviewId
        );

        int totalQuestions =
                (int) questionRepository.countByInterviewId(interviewId);

        // Only answered questions are sent to the AI.
        List<Answer> answers =
                answerRepository
                        .findByQuestionInterviewIdOrderByIdAsc(interviewId);

        int answeredQuestions = answers.size();

        int skippedQuestions =
                Math.max(
                        0,
                        totalQuestions - answeredQuestions
                );

        OverallResult result = new OverallResult();

        result.totalQuestions = totalQuestions;
        result.answeredQuestions = answeredQuestions;
        result.skippedQuestions = skippedQuestions;

        // No answers means there is nothing to evaluate.
        if (answers.isEmpty()) {

            result.aiAvailable = false;
            result.score = 0;
            result.confidence = 0;

            result.weaknesses.add(
                    "No questions were answered."
            );

            result.recommendations.add(
                    "Try to answer at least one question "
                            + "to receive an AI evaluation."
            );

            interview.setStatus(InterviewStatus.FAILED);
            interviewRepository.save(interview);

            return result;
        }

        if (skippedQuestions > 0) {

            result.weaknesses.add(
                    "You skipped "
                            + skippedQuestions
                            + " of "
                            + totalQuestions
                            + " questions."
            );

            result.recommendations.add(
                    "Try to answer more questions when possible. "
                            + "Even a concise answer is better than skipping."
            );
        }

        List<String> questions = new ArrayList<>();
        List<String> candidateAnswers = new ArrayList<>();

        for (Answer answer : answers) {

            questions.add(
                    answer.getQuestion().getQuestionText()
            );

            candidateAnswers.add(
                    answer.getAnswerText()
            );
        }

        // One AI request evaluates all answered questions.
        List<AIService.AIEvaluation> evaluations =
                aiService.evaluateInterviewWithAI(
                        questions,
                        candidateAnswers
                );

        boolean aiAvailable =
                evaluations != null
                        && evaluations.size() == answers.size()
                        && evaluations.stream()
                        .allMatch(evaluation -> evaluation != null);

        result.aiAvailable = aiAvailable;

        if (!aiAvailable) {

            log.warn(
                    "AI evaluation unavailable for interview {}",
                    interviewId
            );

            result.weaknesses.add(
                    "AI evaluation is currently unavailable."
            );

            result.recommendations.add(
                    "Please try evaluating the interview again later."
            );

            interview.setStatus(InterviewStatus.FAILED);
            interviewRepository.save(interview);

            return result;
        }

        int totalScore = 0;
        int totalConfidence = 0;
        int validEvaluations = 0;

        for (int i = 0; i < answers.size(); i++) {

            Answer answer = answers.get(i);
            AIService.AIEvaluation evaluation = evaluations.get(i);

            if (evaluation == null) {
                throw new IllegalStateException(
                        "AI evaluation missing for answer "
                                + answer.getId()
                );
            }

            totalScore += evaluation.score;
            totalConfidence += evaluation.confidence;
            validEvaluations++;

            result.fillerWords += evaluation.fillerWords;

            if (evaluation.relevance != null) {
                result.relevance = evaluation.relevance;
            }

            if (evaluation.strengths != null) {
                result.strengths.addAll(evaluation.strengths);
            }

            if (evaluation.weaknesses != null) {
                result.weaknesses.addAll(evaluation.weaknesses);
            }

            if (evaluation.recommendations != null) {
                result.recommendations.addAll(
                        evaluation.recommendations
                );
            }

            Evaluation savedEvaluation =
                    evaluationRepository
                            .findByAnswerId(answer.getId())
                            .orElseGet(Evaluation::new);

            savedEvaluation.setAnswer(answer);
            savedEvaluation.setScore(evaluation.score);

            savedEvaluation.setFillerWords(
                    Math.max(0, evaluation.fillerWords)
            );

            savedEvaluation.setRelevance(
                    evaluation.relevance != null
                            ? evaluation.relevance
                            : "medium"
            );

            savedEvaluation.setTechnicalAccuracy(
                    evaluation.technicalAccuracy
            );

            savedEvaluation.setStrengths(
                    toJson(
                            evaluation.strengths != null
                                    ? evaluation.strengths
                                    : new ArrayList<>()
                    )
            );

            savedEvaluation.setWeaknesses(
                    toJson(
                            evaluation.weaknesses != null
                                    ? evaluation.weaknesses
                                    : new ArrayList<>()
                    )
            );

            savedEvaluation.setRecommendations(
                    toJson(
                            evaluation.recommendations != null
                                    ? evaluation.recommendations
                                    : new ArrayList<>()
                    )
            );

            evaluationRepository.save(savedEvaluation);
        }

        if (validEvaluations == 0) {

            result.aiAvailable = false;

            result.weaknesses.add(
                    "No valid AI evaluations were returned."
            );

            result.recommendations.add(
                    "Please try evaluating the interview again."
            );

            interview.setStatus(InterviewStatus.FAILED);
            interviewRepository.save(interview);

            return result;
        }

        result.score =
                totalScore / validEvaluations;

        result.confidence =
                totalConfidence / validEvaluations;

        interview.setFinalScore(result.score);
        interview.setCompletedAt(LocalDateTime.now());
        interview.setStatus(InterviewStatus.EVALUATED);

        interviewRepository.save(interview);

        log.info(
                "Interview {} evaluated successfully: score={}, answered={}, skipped={}",
                interviewId,
                result.score,
                result.answeredQuestions,
                result.skippedQuestions
        );

        return result;
    }

    @Transactional(readOnly = true)
    public List<Evaluation> getInterviewEvaluations(
            Long interviewId) {

        return evaluationRepository
                .findByAnswerQuestionInterviewIdOrderByIdAsc(
                        interviewId
                );
    }

    @Transactional(readOnly = true)
    public List<Evaluationsdtos.EvaluationItemResponse>
    getInterviewEvaluationDetails(Long interviewId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewIdOrderByIdAsc(
                                interviewId
                        );

        List<Evaluationsdtos.EvaluationItemResponse> response =
                new ArrayList<>();

        for (Evaluation evaluation : evaluations) {

            Answer answer = evaluation.getAnswer();

            try {

                List<String> strengths =
                        objectMapper.readValue(
                                evaluation.getStrengths(),
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                String.class
                                        )
                        );

                List<String> weaknesses =
                        objectMapper.readValue(
                                evaluation.getWeaknesses(),
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                String.class
                                        )
                        );

                List<String> recommendations =
                        objectMapper.readValue(
                                evaluation.getRecommendations(),
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(
                                                List.class,
                                                String.class
                                        )
                        );

                response.add(
                        new Evaluationsdtos.EvaluationItemResponse(
                                answer.getQuestion().getId(),
                                answer.getQuestion().getQuestionText(),
                                evaluation.getScore(),
                                evaluation.getFillerWords(),
                                evaluation.getRelevance(),
                                evaluation.getTechnicalAccuracy(),
                                strengths,
                                weaknesses,
                                recommendations
                        )
                );

            } catch (Exception e) {

                throw new RuntimeException(
                        "Failed to parse stored evaluation data",
                        e
                );
            }
        }

        return response;
    }

    private String toJson(Object value) {

        try {

            return objectMapper.writeValueAsString(value);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to convert evaluation data to JSON",
                    e
            );
        }
    }
}