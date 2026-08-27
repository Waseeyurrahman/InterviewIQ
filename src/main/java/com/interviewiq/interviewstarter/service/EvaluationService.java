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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {

    private final AIService aiService;
    private final EvaluationRepository evaluationRepository;
    private final AnswerRepository answerRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final QuestionRepository questionRepository;

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


    /*
     * ============================================================
     * OVERALL RESULT
     * ============================================================
     */
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


    /*
     * ============================================================
     * EVALUATE ENTIRE INTERVIEW
     * ============================================================
     *
     * Important:
     *
     * We evaluate ONLY questions for which an Answer exists.
     *
     * Example:
     *
     * 5 questions
     * 3 answers
     *
     * Gemini receives:
     *
     * Question 1 + Answer 1
     * Question 2 + Answer 2
     * Question 4 + Answer 4
     *
     * One Gemini request.
     *
     * The skipped questions are NOT sent to Gemini.
     *
     */
    @Transactional
    public OverallResult evaluateInterview(Long interviewId) {

        /*
         * ========================================================
         * STEP 1 — Find interview
         * ========================================================
         */

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

        int claimed = 0;

        if (interview.getStatus() == InterviewStatus.COMPLETED) {

            claimed = interviewRepository.updateStatusIfCurrent(
                    interviewId,
                    InterviewStatus.COMPLETED,
                    InterviewStatus.EVALUATING
            );

        } else if (interview.getStatus() == InterviewStatus.FAILED) {

            claimed = interviewRepository.updateStatusIfCurrent(
                    interviewId,
                    InterviewStatus.FAILED,
                    InterviewStatus.EVALUATING
            );
        }

        if (claimed != 1) {
            throw new IllegalStateException(
                    "Interview is already being evaluated or has already been evaluated"
            );
        }
        interview.setStatus(InterviewStatus.EVALUATING);



        /*
         * ========================================================
         * STEP 2 — Get all generated questions
         * ========================================================
         */

        int totalQuestions =
                (int) questionRepository.countByInterviewId(interviewId);


        /*
         * ========================================================
         * STEP 3 — Get only answered questions
         * ========================================================
         */

        List<Answer> answers =
                answerRepository
                        .findByQuestionInterviewIdOrderByIdAsc(interviewId);


        int answeredQuestions =
                answers.size();

        int skippedQuestions =
                Math.max(
                        0,
                        totalQuestions - answeredQuestions
                );


        /*
         * ========================================================
         * STEP 4 — Create result
         * ========================================================
         */

        OverallResult result =
                new OverallResult();

        result.totalQuestions =
                totalQuestions;

        result.answeredQuestions =
                answeredQuestions;

        result.skippedQuestions =
                skippedQuestions;


        /*
         * ========================================================
         * STEP 5 — No answers
         * ========================================================
         */

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

        interview.setStatus(InterviewStatus.EVALUATING);
        interviewRepository.save(interview);


        /*
         * ========================================================
         * STEP 6 — Add skipped-question feedback
         * ========================================================
         */

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


        /*
         * ========================================================
         * STEP 7 — Prepare answered Q&A pairs
         * ========================================================
         */

        List<String> questions =
                new ArrayList<>();

        List<String> candidateAnswers =
                new ArrayList<>();


        for (Answer answer : answers) {

            questions.add(
                    answer.getQuestion().getQuestionText()
            );

            candidateAnswers.add(
                    answer.getAnswerText()
            );
        }


        /*
         * ========================================================
         * STEP 8 — ONE GEMINI REQUEST
         * ========================================================
         *
         * IMPORTANT:
         *
         * Only answered questions are sent.
         *
         * Example:
         *
         * 5 generated
         * 3 answered
         *
         * Gemini receives exactly:
         *
         * Q1 + A1
         * Q2 + A2
         * Q4 + A4
         *
         * One API call.
         */

        List<AIService.AIEvaluation> evaluations =
                aiService.evaluateInterviewWithAI(
                        questions,
                        candidateAnswers
                );


        /*
         * ========================================================
         * STEP 9 — Validate Gemini result
         * ========================================================
         */

        boolean aiAvailable =
                evaluations != null
                        && evaluations.size() == answers.size()
                        && evaluations.stream()
                        .allMatch(evaluation -> evaluation != null);

        result.aiAvailable =
                aiAvailable;


        if (!aiAvailable) {

            System.err.println(
                    "[EvaluationService] AI evaluation unavailable."
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


        /*
         * ========================================================
         * STEP 10 — Process AI evaluations
         * ========================================================
         */

        int totalScore = 0;
        int totalConfidence = 0;
        int validEvaluations = 0;


        for (int i = 0; i < answers.size(); i++) {

            Answer answer =
                    answers.get(i);

            AIService.AIEvaluation evaluation =
                    evaluations.get(i);


            if (evaluation == null) {

                throw new IllegalStateException(
                        "AI evaluation missing for answer "
                                + answer.getId()
                );
            }


            /*
             * ====================================================
             * SCORE
             * ====================================================
             */

            int score =
                    evaluation.score;

            totalScore += score;


            /*
             * ====================================================
             * CONFIDENCE
             * ====================================================
             */

            int confidence =
                    evaluation.confidence;

            totalConfidence += confidence;

            validEvaluations++;


            /*
             * ====================================================
             * OVERALL DATA
             * ====================================================
             */

            result.fillerWords +=
                    evaluation.fillerWords;


            if (evaluation.relevance != null) {

                result.relevance =
                        evaluation.relevance;
            }


            if (evaluation.strengths != null) {

                result.strengths.addAll(
                        evaluation.strengths
                );
            }


            if (evaluation.weaknesses != null) {

                result.weaknesses.addAll(
                        evaluation.weaknesses
                );
            }


            if (evaluation.recommendations != null) {

                result.recommendations.addAll(
                        evaluation.recommendations
                );
            }


            /*
             * ====================================================
             * FIND EXISTING EVALUATION
             * ====================================================
             */

            Evaluation savedEvaluation =
                    evaluationRepository
                            .findByAnswerId(answer.getId())
                            .orElseGet(
                                    Evaluation::new
                            );


            /*
             * ====================================================
             * LINK ANSWER
             * ====================================================
             */

            savedEvaluation.setAnswer(
                    answer
            );


            /*
             * ====================================================
             * SAVE SCORE
             * ====================================================
             */

            savedEvaluation.setScore(
                    score
            );


            /*
             * ====================================================
             * SAVE FILLER WORDS
             * ====================================================
             */

            savedEvaluation.setFillerWords(
                    Math.max(
                            0,
                            evaluation.fillerWords
                    )
            );


            /*
             * ====================================================
             * SAVE RELEVANCE
             * ====================================================
             */

            savedEvaluation.setRelevance(
                    evaluation.relevance != null
                            ? evaluation.relevance
                            : "medium"
            );


            /*
             * ====================================================
             * SAVE TECHNICAL ACCURACY
             * ====================================================
             */

            savedEvaluation.setTechnicalAccuracy(
                    evaluation.technicalAccuracy
            );


            /*
             * ====================================================
             * SAVE STRENGTHS
             * ====================================================
             */

            savedEvaluation.setStrengths(
                    toJson(
                            evaluation.strengths != null
                                    ? evaluation.strengths
                                    : new ArrayList<>()
                    )
            );


            /*
             * ====================================================
             * SAVE WEAKNESSES
             * ====================================================
             */

            savedEvaluation.setWeaknesses(
                    toJson(
                            evaluation.weaknesses != null
                                    ? evaluation.weaknesses
                                    : new ArrayList<>()
                    )
            );


            /*
             * ====================================================
             * SAVE RECOMMENDATIONS
             * ====================================================
             */

            savedEvaluation.setRecommendations(
                    toJson(
                            evaluation.recommendations != null
                                    ? evaluation.recommendations
                                    : new ArrayList<>()
                    )
            );


            /*
             * ====================================================
             * SAVE
             * ====================================================
             */

            evaluationRepository.save(
                    savedEvaluation
            );
        }


        /*
         * ========================================================
         * STEP 11 — Make sure at least one evaluation succeeded
         * ========================================================
         */

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


        /*
         * ========================================================
         * STEP 12 — Calculate overall score
         * ========================================================
         *
         * ONLY answered questions are included.
         */

        result.score =
                totalScore / validEvaluations;

        result.confidence =
                totalConfidence / validEvaluations;


        /*
         * ========================================================
         * STEP 13 — Mark interview COMPLETED
         * ========================================================
         *
         * This happens ONLY after successful AI evaluation.
         */

        interview.setFinalScore(
                result.score
        );

        interview.setCompletedAt(
                LocalDateTime.now()
        );

        interview.setStatus(
                InterviewStatus.EVALUATED
        );

        interviewRepository.save(
                interview
        );


        /*
         * ========================================================
         * STEP 14 — Return result
         * ========================================================
         */

        return result;
    }


    /*
     * ============================================================
     * GET ALL EVALUATIONS
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<Evaluation> getInterviewEvaluations(
            Long interviewId) {

        return evaluationRepository
                .findByAnswerQuestionInterviewIdOrderByIdAsc(
                        interviewId
                );
    }


    /*
     * ============================================================
     * GET DETAILED EVALUATION RESPONSE
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<Evaluationsdtos.EvaluationItemResponse>
    getInterviewEvaluationDetails(
            Long interviewId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewIdOrderByIdAsc(
                                interviewId
                        );


        List<Evaluationsdtos.EvaluationItemResponse> response =
                new ArrayList<>();


        for (Evaluation evaluation : evaluations) {

            Answer answer =
                    evaluation.getAnswer();


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


    /*
     * ============================================================
     * LIST → JSON
     * ============================================================
     */

    private String toJson(Object value) {

        try {

            return objectMapper.writeValueAsString(
                    value
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to convert evaluation data to JSON",
                    e
            );
        }
    }
}