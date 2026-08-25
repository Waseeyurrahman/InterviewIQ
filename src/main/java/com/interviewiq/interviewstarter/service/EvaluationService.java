package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.dto.Evaluationsdtos;
import com.interviewiq.interviewstarter.entity.Answer;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {

    private final AIService aiService;
    private final EvaluationRepository evaluationRepository;
    private final AnswerRepository answerRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            AIService aiService,
            EvaluationRepository evaluationRepository,
            AnswerRepository answerRepository,
            InterviewRepository interviewRepository,
            ObjectMapper objectMapper) {

        this.aiService = aiService;
        this.evaluationRepository = evaluationRepository;
        this.answerRepository = answerRepository;
        this.objectMapper = objectMapper;
        this.interviewRepository = interviewRepository;
    }

    public static class OverallResult {

        public boolean aiAvailable;


        public int score;
        public int fillerWords;
        public int confidence;

        public String relevance = "medium";

        public List<String> strengths = new ArrayList<>();
        public List<String> weaknesses = new ArrayList<>();
        public List<String> recommendations = new ArrayList<>();
    }

    @Transactional
    public OverallResult evaluateInterview(Long interviewId) {

        List<Answer> answers =
                answerRepository
                        .findByQuestionInterviewIdOrderByIdAsc(interviewId);

        OverallResult result = new OverallResult();

        if (answers.isEmpty()) {

            result.aiAvailable = false;
            result.weaknesses.add(
                    "No answers were provided"
            );

            result.recommendations.add(
                    "Complete the interview before evaluating"
            );

            return result;
        }

        /*
         * ============================================================
         * STEP 1 — Collect all questions and answers
         * ============================================================
         */

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


        /*
         * ============================================================
         * STEP 2 — ONE Gemini request for the entire interview
         * ============================================================
         */

        List<AIService.AIEvaluation> evaluations =
                aiService.evaluateInterviewWithAI(
                        questions,
                        candidateAnswers
                );


        /*
         * ============================================================
         * STEP 3 — Check whether Gemini returned valid results
         * ============================================================
         */

        boolean aiAvailable =
                evaluations != null &&
                        evaluations.size() == answers.size();
        if (aiAvailable) {
            result.aiAvailable = true;
        }


        int totalScore = 0;


        /*
         * ============================================================
         * STEP 4 — Process every answer
         * ============================================================
         */

        for (int i = 0; i < answers.size(); i++) {

            Answer answer = answers.get(i);

            AIService.AIEvaluation evaluation;

            if (!aiAvailable) {

                System.err.println(
                        "[EvaluationService] AI evaluation unavailable."
                );
                result.aiAvailable = false;

                result.weaknesses.add(
                        "AI evaluation is currently unavailable."
                );

                result.recommendations.add(
                        "Please try evaluating the interview again later."
                );

                return result;
            }

            evaluation = evaluations.get(i);

            int score = 0;


            /*
             * ========================================================
             * AI RESULT AVAILABLE
             * ========================================================
             */

            if (evaluation != null) {

                score =
                        evaluation.score;


                /*
                 * Add strengths.
                 */

                if (evaluation.strengths != null) {

                    result.strengths.addAll(
                            evaluation.strengths
                    );
                }


                /*
                 * Add weaknesses.
                 */

                if (evaluation.weaknesses != null) {

                    result.weaknesses.addAll(
                            evaluation.weaknesses
                    );
                }


                /*
                 * Add recommendations.
                 */

                if (evaluation.recommendations != null) {

                    result.recommendations.addAll(
                            evaluation.recommendations
                    );
                }


                /*
                 * Store relevance.
                 *
                 * We will improve the overall relevance calculation later.
                 */

                if (evaluation.relevance != null) {

                    result.relevance =
                            evaluation.relevance;
                }


                /*
                 * Add filler words.
                 */

                result.fillerWords +=
                        evaluation.fillerWords;
            }


            totalScore += score;


            /*
             * ============================================================
             * STEP 5 — Find existing evaluation
             * ============================================================
             *
             * If this answer has already been evaluated:
             *
             *     UPDATE existing row
             *
             * Otherwise:
             *
             *     CREATE new row
             */

            Evaluation savedEvaluation =
                    evaluationRepository
                            .findByAnswerId(answer.getId())
                            .orElseGet(
                                    Evaluation::new
                            );


            savedEvaluation.setAnswer(answer);

            savedEvaluation.setScore(score);


            /*
             * ============================================================
             * STEP 6 — Save AI evaluation details
             * ============================================================
             */

            savedEvaluation.setFillerWords(
                    evaluation.fillerWords
            );

            savedEvaluation.setRelevance(
                    evaluation.relevance
            );

            savedEvaluation.setTechnicalAccuracy(
                    evaluation.technicalAccuracy
            );

            savedEvaluation.setStrengths(
                    toJson(evaluation.strengths)
            );

            savedEvaluation.setWeaknesses(
                    toJson(evaluation.weaknesses)
            );

            savedEvaluation.setRecommendations(
                    toJson(evaluation.recommendations)
            );


            /*
             * Save evaluation.
             */

            evaluationRepository.save(
                    savedEvaluation
            );
        }


        /*
         * ============================================================
         * STEP 7 — Calculate overall score
         * ============================================================
         */

        result.score =
                totalScore / answers.size();


        /*
         * Temporary confidence calculation.
         *
         * Later we will calculate confidence separately.
         */

        result.confidence =
                result.score;

        Interview interview =
                interviewRepository.findById(interviewId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Interview not found: " + interviewId
                                )
                        );

        interview.setFinalScore(result.score);
        interview.setCompletedAt(
                java.time.LocalDateTime.now()
        );

        interviewRepository.save(interview);

        return result;
    }






    @Transactional(readOnly = true)
    public List<Evaluation> getInterviewEvaluations(Long interviewId) {

        return evaluationRepository
                .findByAnswerQuestionInterviewIdOrderByIdAsc(interviewId);
    }

    /*
     * Convert Java List into JSON String
     * for storing in the database.
     */
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

    @Transactional(readOnly = true)
    public List<Evaluationsdtos.EvaluationItemResponse> getInterviewEvaluationDetails(
            Long interviewId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewIdOrderByIdAsc(interviewId);

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
}