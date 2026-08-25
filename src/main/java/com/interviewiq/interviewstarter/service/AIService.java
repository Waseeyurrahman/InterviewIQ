package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {

//    private static final String GEMINI_URL =
//            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
private static final String GEMINI_BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${ai.gemini.model}")
    private String model;

    private int geminiCallCount = 0;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ============================================================
    // AI EVALUATION RESULT
    // ============================================================

    public static class AIEvaluation {

        public int score;

        public int fillerWords;

        public String relevance;

        public String technicalAccuracy;

        public List<String> strengths = new ArrayList<>();

        public List<String> weaknesses = new ArrayList<>();

        public List<String> recommendations = new ArrayList<>();
    }


    // ============================================================
    // EVALUATE CANDIDATE ANSWER
    // ============================================================

    public AIEvaluation evaluateWithAI(
            String question,
            String answer) {

        if (apiKey == null || apiKey.isBlank()) {

            System.err.println(
                    "[AIService] Gemini API key is missing."
            );

            return null;
        }

        try {

            String prompt =
                    buildEvaluationPrompt(
                            question,
                            answer
                    );

            String aiResponse =
                    callGemini(prompt);

            AIEvaluation evaluation =
                    parseEvaluation(aiResponse);

            if (evaluation == null) {

                System.err.println(
                        "[AIService] Failed to parse AI evaluation."
                );

                return null;
            }

            validateEvaluation(evaluation);

            return evaluation;

        } catch (Exception e) {

            System.err.println(
                    "[AIService] AI evaluation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }

    // ============================================================
// EVALUATE MULTIPLE QUESTION + ANSWER PAIRS
// ============================================================

    public List<AIEvaluation> evaluateInterviewWithAI(
            List<String> questions,
            List<String> answers) {

        if (apiKey == null || apiKey.isBlank()) {

            System.err.println(
                    "[AIService] Gemini API key is missing."
            );

            return null;
        }

        if (questions == null ||
                answers == null ||
                questions.size() != answers.size() ||
                questions.isEmpty()) {

            System.err.println(
                    "[AIService] Invalid questions/answers."
            );

            return null;
        }

        try {

            String prompt =
                    buildBatchEvaluationPrompt(
                            questions,
                            answers
                    );

            String aiResponse =
                    callGemini(prompt);

            return parseBatchEvaluation(aiResponse);

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Batch AI evaluation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }


    // ============================================================
    // GENERATE INTERVIEW QUESTIONS
    // ============================================================

    public List<String> generateQuestions(
            String role,
            String experienceLevel,
            String difficulty,
            int count) {

        if (apiKey == null || apiKey.isBlank()) {

            System.err.println(
                    "[AIService] Gemini API key is missing."
            );

            return null;
        }

        try {

            String prompt =
                    buildQuestionPrompt(
                            role,
                            experienceLevel,
                            difficulty,
                            count
                    );

            String aiResponse =
                    callGemini(prompt);

            return parseQuestionList(aiResponse);

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Question generation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }


    // ============================================================
    // EVALUATION PROMPT
    // ============================================================

    private String buildEvaluationPrompt(
            String question,
            String answer) {

        return """
                ROLE:
                You are an expert technical interviewer evaluating
                a software engineering candidate.

                OBJECTIVE:
                Evaluate ONLY the candidate's answer to the question.
                Do not assume knowledge that the candidate did not demonstrate.

                QUESTION:
                %s

                CANDIDATE ANSWER:
                %s

                EVALUATION DIMENSIONS:

                1. RELEVANCE
                Does the answer directly address the question?

                2. TECHNICAL ACCURACY
                Are the technical statements correct?

                3. CLARITY
                Is the explanation understandable, logically structured,
                and appropriately concise?

                4. COMPLETENESS
                Did the candidate address the important parts of the question?

                SCORING:

                Return an integer score from 0 to 100.

                90-100 = Excellent
                75-89  = Good
                60-74  = Average
                40-59  = Weak
                0-39   = Poor

                IMPORTANT SCORING RULES:

                - A short but correct answer can receive a high score.
                - Do not reward unnecessary verbosity.
                - Do not penalize the candidate for information that
                  the question did not require.
                - Do not invent missing information.
                - Judge only what the candidate actually said.
                - Technical errors should reduce the score.
                - If multiple parts were requested and only some were answered,
                  reduce the score accordingly.
                - Minor wording mistakes should not be treated as technical errors.

                FILLER WORDS:

                Count actual filler-word usage.

                Filler words:
                um
                uh
                er
                like
                you know
                basically

                Do NOT count a word when it has normal semantic meaning.

                RELEVANCE:

                Return exactly one:

                low
                medium
                high

                TECHNICAL ACCURACY:

                Return exactly one:

                poor
                average
                good

                STRENGTHS:

                Give 2-4 specific strengths supported by the answer.

                WEAKNESSES:

                Give 1-4 specific weaknesses supported by the answer.

                RECOMMENDATIONS:

                Give 1-4 actionable recommendations that would help
                the candidate improve this answer.

                OUTPUT FORMAT:

                Return ONLY valid JSON.

                {
                  "score": 0,
                  "fillerWords": 0,
                  "relevance": "medium",
                  "technicalAccuracy": "average",
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": []
                }

                Do not return Markdown.
                Do not return ```json.
                Do not include explanations outside the JSON.
                """.formatted(
                question,
                answer
        );
    }

    // ============================================================
// BATCH EVALUATION PROMPT
// ============================================================

    private String buildBatchEvaluationPrompt(
            List<String> questions,
            List<String> answers) {

        StringBuilder input = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {

            input.append("\nQUESTION ")
                    .append(i + 1)
                    .append(":\n")
                    .append(questions.get(i))
                    .append("\n");

            input.append("CANDIDATE ANSWER ")
                    .append(i + 1)
                    .append(":\n")
                    .append(answers.get(i))
                    .append("\n");

            input.append("-------------------------\n");
        }

        return """
            ROLE:
            You are an expert technical interviewer evaluating
            a software engineering candidate.

            TASK:
            Evaluate every question and answer pair separately.

                IMPORTANT:
                - Evaluation 1 corresponds to Question 1.
                - Evaluation 2 corresponds to Question 2.
                - Continue in the same order.
                - Do not skip any question.
                - Do not combine multiple answers into one evaluation.
                - Evaluate only what the candidate actually said.

            EVALUATION CRITERIA:

            1. RELEVANCE
            Does the answer directly address the question?

            2. TECHNICAL ACCURACY
            Are the technical statements correct?

            3. CLARITY
            Is the explanation understandable and logically structured?

            4. COMPLETENESS
            Did the candidate address the important parts of the question?

            SCORING:

            90-100 = Excellent
            75-89  = Good
            60-74  = Average
            40-59  = Weak
            0-39   = Poor

            FILLER WORDS:

            Count actual filler-word usage:

            um
            uh
            er
            like
            you know
            basically

            Only count them when they are used as filler words.

            RELEVANCE:

            Use exactly:

            low
            medium
            high

            TECHNICAL ACCURACY:

            Use exactly:

            poor
            average
            good

            STRENGTHS:

            Provide 2-4 specific strengths.

            WEAKNESSES:

            Provide 1-4 specific weaknesses.

            RECOMMENDATIONS:

            Provide 1-4 actionable recommendations.

            QUESTION AND ANSWER PAIRS:

            %s

            OUTPUT:

            Return ONLY a valid JSON array.

            [
              {
                "score": 75,
                "fillerWords": 0,
                "relevance": "high",
                "technicalAccuracy": "good",
                "strengths": [],
                "weaknesses": [],
                "recommendations": []
              }
            ]

            The number of objects MUST equal the number of
            question-answer pairs.

            Do not return Markdown.
            Do not return ```json.
            Do not include any text outside the JSON array.
            """.formatted(
                input
        );
    }


    // ============================================================
    // QUESTION GENERATION PROMPT
    // ============================================================

    private String buildQuestionPrompt(
            String role,
            String experienceLevel,
            String difficulty,
            int count) {

        return """
                ROLE:
                You are an expert technical interviewer.

                TASK:
                Generate %d interview questions for the candidate.

                CANDIDATE ROLE:
                %s

                EXPERIENCE LEVEL:
                %s

                DIFFICULTY:
                %s

                REQUIREMENTS:

                - Questions must be relevant to the candidate's role.
                - Questions must match the requested experience level.
                - Questions must match the requested difficulty.
                - Avoid duplicate questions.
                - Avoid vague questions.
                - Prefer practical technical interview questions.
                - Questions should test understanding rather than memorization.
                - Do not provide answers.
                - Do not number the questions.

                OUTPUT:

                Return ONLY a valid JSON array of strings.

                Example:

                [
                  "What is dependency injection in Spring?",
                  "Explain the difference between HashMap and ConcurrentHashMap.",
                  "How does JWT authentication work?"
                ]

                Do not return Markdown.
                Do not return ```json.
                Do not include any text outside the JSON array.
                """.formatted(
                count,
                role,
                experienceLevel,
                difficulty
        );
    }


    // ============================================================
    // CALL GEMINI
    // ============================================================


    private String callGemini(String prompt)

            throws Exception {

        String body =
                """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": %s
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                        objectMapper.writeValueAsString(prompt)
                );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<String> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        try {

            String url =
                    GEMINI_BASE_URL
                            + model
                            + ":generateContent?key="
                            + apiKey;

            System.out.println("=== GEMINI REQUEST ===");
            System.out.println("Model: " + model);
            System.out.println("URL: " + GEMINI_BASE_URL + model + ":generateContent");
            System.out.println("Calling Gemini...");


            geminiCallCount++;

            System.out.println(
                    "=== GEMINI API CALL #" + geminiCallCount + " ==="
            );
            ResponseEntity<String> response =
                    http.exchange(
                            url,
                            HttpMethod.POST,
                            request,
                            String.class
                    );
            System.out.println("=== GEMINI RESPONSE RECEIVED ===");
            if (response.getBody() == null ||
                    response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Gemini returned an empty response."
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.getBody()
                    );

            JsonNode textNode =
                    root.path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text");

            if (textNode.isMissingNode() ||
                    textNode.isNull()) {

                throw new RuntimeException(
                        "Gemini response does not contain text: "
                                + response.getBody()
                );
            }

            return textNode.asText();

        } catch (HttpStatusCodeException e) {

            System.err.println(
                    "Gemini HTTP status: "
                            + e.getStatusCode()
            );

            System.err.println(
                    "Gemini response: "
                            + e.getResponseBodyAsString()
            );

            throw e;
        }
    }


    // ============================================================
    // PARSE EVALUATION
    // ============================================================

    private AIEvaluation parseEvaluation(
            String aiText) {

        try {

            String clean =
                    cleanJsonResponse(aiText);

            JsonNode root =
                    objectMapper.readTree(clean);

            AIEvaluation evaluation =
                    new AIEvaluation();

            evaluation.score =
                    root.path("score").asInt(0);

            evaluation.fillerWords =
                    root.path("fillerWords").asInt(0);

            evaluation.relevance =
                    root.path("relevance")
                            .asText("medium");

            evaluation.technicalAccuracy =
                    root.path("technicalAccuracy")
                            .asText("average");

            evaluation.strengths =
                    readStringList(
                            root.path("strengths")
                    );

            evaluation.weaknesses =
                    readStringList(
                            root.path("weaknesses")
                    );

            evaluation.recommendations =
                    readStringList(
                            root.path("recommendations")
                    );

            return evaluation;

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Evaluation JSON parsing failed."
            );

            return null;
        }
    }

    // ============================================================
// PARSE BATCH EVALUATIONS
// ============================================================

    private List<AIEvaluation> parseBatchEvaluation(
            String aiText) {

        try {

            String clean =
                    cleanJsonResponse(aiText);

            JsonNode root =
                    objectMapper.readTree(clean);

            if (!root.isArray()) {

                System.err.println(
                        "[AIService] Batch response is not an array."
                );

                return null;
            }

            List<AIEvaluation> evaluations =
                    new ArrayList<>();

            for (JsonNode node : root) {

                AIEvaluation evaluation =
                        new AIEvaluation();

                evaluation.score =
                        node.path("score").asInt(0);

                evaluation.fillerWords =
                        node.path("fillerWords").asInt(0);

                evaluation.relevance =
                        node.path("relevance")
                                .asText("medium");

                evaluation.technicalAccuracy =
                        node.path("technicalAccuracy")
                                .asText("average");

                evaluation.strengths =
                        readStringList(
                                node.path("strengths")
                        );

                evaluation.weaknesses =
                        readStringList(
                                node.path("weaknesses")
                        );

                evaluation.recommendations =
                        readStringList(
                                node.path("recommendations")
                        );

                validateEvaluation(evaluation);

                evaluations.add(evaluation);
            }

            return evaluations.isEmpty()
                    ? null
                    : evaluations;

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Batch evaluation JSON parsing failed."
            );

            return null;
        }
    }


    // ============================================================
    // PARSE QUESTION LIST
    // ============================================================

    private List<String> parseQuestionList(
            String aiText) {

        try {

            String clean =
                    cleanJsonResponse(aiText);

            JsonNode root =
                    objectMapper.readTree(clean);

            if (!root.isArray()) {

                return null;
            }

            List<String> questions =
                    new ArrayList<>();

            for (JsonNode node : root) {

                if (node.isTextual()) {

                    String question =
                            node.asText().trim();

                    if (!question.isBlank()) {

                        questions.add(question);
                    }
                }
            }

            return questions.isEmpty()
                    ? null
                    : questions;

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Question JSON parsing failed."
            );

            return null;
        }
    }


    // ============================================================
    // READ JSON STRING ARRAY
    // ============================================================

    private List<String> readStringList(
            JsonNode node) {

        List<String> result =
                new ArrayList<>();

        if (node != null && node.isArray()) {

            for (JsonNode item : node) {

                if (item.isTextual()) {

                    String value =
                            item.asText().trim();

                    if (!value.isBlank()) {

                        result.add(value);
                    }
                }
            }
        }

        return result;
    }


    // ============================================================
    // CLEAN AI JSON RESPONSE
    // ============================================================

    private String cleanJsonResponse(
            String text) {

        if (text == null) {

            throw new IllegalArgumentException(
                    "AI response is null"
            );
        }

        String clean =
                text.trim();

        // Remove Markdown code fences
        if (clean.startsWith("```")) {

            clean =
                    clean.replaceFirst(
                            "^```(?:json)?\\s*",
                            ""
                    );

            clean =
                    clean.replaceFirst(
                            "\\s*```$",
                            ""
                    );
        }

        return clean.trim();
    }


    // ============================================================
    // VALIDATE AI EVALUATION
    // ============================================================

    private void validateEvaluation(
            AIEvaluation evaluation) {

        // Protect our application from invalid AI scores.

        if (evaluation.score < 0) {

            evaluation.score = 0;
        }

        if (evaluation.score > 100) {

            evaluation.score = 100;
        }

        if (evaluation.fillerWords < 0) {

            evaluation.fillerWords = 0;
        }

        if (evaluation.relevance == null ||
                !List.of(
                        "low",
                        "medium",
                        "high"
                ).contains(
                        evaluation.relevance.toLowerCase()
                )) {

            evaluation.relevance =
                    "medium";
        }

        if (evaluation.technicalAccuracy == null ||
                !List.of(
                        "poor",
                        "average",
                        "good"
                ).contains(
                        evaluation.technicalAccuracy.toLowerCase()
                )) {

            evaluation.technicalAccuracy =
                    "average";
        }
    }
}