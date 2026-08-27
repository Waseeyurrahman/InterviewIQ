package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AIService {

    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${ai.gemini.model}")
    private String model;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    private final AtomicInteger geminiCallCount =
            new AtomicInteger(0);

    private final RestTemplate http;

    public AIService() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        this.http = new RestTemplate(factory);
    }
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ============================================================
    // AI EVALUATION RESULT
    // ============================================================

    public static class AIEvaluation {

        public int score;

        public int fillerWords;

        public int confidence;

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

            return parseBatchEvaluation(
                    aiResponse,
                    questions.size()
            );

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

            List<String> questions =
                    parseQuestionList(aiResponse);

            if (questions == null ||
                    questions.size() != count) {

                System.err.println(
                        "[AIService] Gemini returned "
                                + (questions == null
                                ? 0
                                : questions.size())
                                + " questions, expected "
                                + count
                );

                return null;
            }

            return questions;

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

                5. CONFIDENCE
                Estimate how confidently the candidate appears to communicate
                the answer based ONLY on the wording and structure of the answer.

                Do not infer personality or real-world confidence.

                SCORING:

                Return an integer score from 0 to 100.

                90-100 = Excellent
                75-89  = Good
                60-74  = Average
                40-59  = Weak
                0-39   = Poor

                CONFIDENCE:

                Return an integer from 0 to 100.

                90-100 = Very confident
                75-89  = Confident
                60-74  = Moderately confident
                40-59  = Low confidence
                0-39   = Very low confidence

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
                  "confidence": 0,
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

            5. CONFIDENCE
            Estimate how confidently the candidate appears to communicate
            the answer based ONLY on the wording and structure of the answer.

            Do not infer personality or real-world confidence.

            SCORING:

            90-100 = Excellent
            75-89  = Good
            60-74  = Average
            40-59  = Weak
            0-39   = Poor

            CONFIDENCE:

            Return an integer from 0 to 100.

            90-100 = Very confident
            75-89  = Confident
            60-74  = Moderately confident
            40-59  = Low confidence
            0-39   = Very low confidence

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
                "confidence": 75,
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
                  ],
                  "generationConfig": {
                    "responseMimeType": "application/json"
                  }
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

        String url =
                GEMINI_BASE_URL
                        + model
                        + ":generateContent?key="
                        + apiKey;

        final int maxAttempts = 3;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try {

                System.out.println(
                        "[AIService] Calling Gemini. Attempt "
                                + attempt
                                + "/"
                                + maxAttempts
                );

                int callNumber =
                        geminiCallCount.incrementAndGet();

                ResponseEntity<String> response =
                        http.exchange(
                                url,
                                HttpMethod.POST,
                                request,
                                String.class
                        );

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
                            "Gemini response does not contain text."
                    );
                }

                System.out.println(
                        "[AIService] Gemini response received successfully."
                );

                return textNode.asText();

            } catch (HttpStatusCodeException e) {

                HttpStatusCode status =
                        e.getStatusCode();

                boolean retryable =
                        status.value() == 429 ||
                                status.value() == 500 ||
                                status.value() == 502 ||
                                status.value() == 503;

                System.err.println(
                        "[AIService] Gemini request failed. HTTP "
                                + status.value()
                                + " (attempt "
                                + attempt
                                + "/"
                                + maxAttempts
                                + ")"
                );

                if (!retryable ||
                        attempt == maxAttempts) {

                    throw e;
                }

                long delay =
                        1000L * (1L << (attempt - 1));

                System.out.println(
                        "[AIService] Retrying Gemini request after "
                                + delay
                                + " ms."
                );

                Thread.sleep(delay);
            }
        }

        throw new IllegalStateException(
                "Gemini request failed after "
                        + maxAttempts
                        + " attempts."
        );
    }


    // ============================================================
    // PARSE EVALUATION
    // ============================================================

    private AIEvaluation parseEvaluation(
            String aiText) {

        try {

            String clean =
                    cleanJsonResponse(aiText);

            System.out.println(
                    "=== CLEAN EVALUATION JSON ==="
            );

            System.out.println(
                    clean
            );

            System.out.println(
                    "=== END CLEAN EVALUATION JSON ==="
            );

            JsonNode root =
                    objectMapper.readTree(clean);

            AIEvaluation evaluation =
                    new AIEvaluation();

            evaluation.score =
                    root.path("score").asInt(0);

            evaluation.fillerWords =
                    root.path("fillerWords").asInt(0);

            evaluation.confidence =
                    root.path("confidence").asInt(0);

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

            System.err.println(
                    "[AIService] Failed to parse AI response: "
                            + e.getMessage()
            );

            return null;
        }
    }


    // ============================================================
    // PARSE BATCH EVALUATIONS
    // ============================================================

    private List<AIEvaluation> parseBatchEvaluation(
            String aiText,
            int expectedCount) {

        try {

            String clean =
                    cleanJsonResponse(aiText);

            /*
             * DEBUGGING:
             *
             * This lets us see the EXACT string that Jackson
             * receives, rather than only the raw Gemini output.
             */

            System.out.println(
                    "=== CLEAN BATCH JSON ==="
            );

            System.out.println(
                    clean
            );

            System.out.println(
                    "=== END CLEAN BATCH JSON ==="
            );

            JsonNode root =
                    objectMapper.readTree(clean);

            if (!root.isArray()) {

                System.err.println(
                        "[AIService] Batch response is not an array."
                );

                return null;
            }

            /*
             * Gemini must return exactly one evaluation
             * for every question-answer pair.
             */

            if (root.size() != expectedCount) {

                System.err.println(
                        "[AIService] Expected "
                                + expectedCount
                                + " evaluations but received "
                                + root.size()
                );

                return null;
            }

            List<AIEvaluation> evaluations =
                    new ArrayList<>();

            for (JsonNode node : root) {

                AIEvaluation evaluation =
                        new AIEvaluation();

                evaluation.score =
                        requireIntField(node, "score");

                evaluation.fillerWords =
                        requireIntField(node, "fillerWords");

                evaluation.confidence =
                        requireIntField(node, "confidence");

                evaluation.relevance =
                        requireStringField(node, "relevance");

                evaluation.technicalAccuracy =
                        requireStringField(
                                node,
                                "technicalAccuracy"
                        );

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

                validateEvaluation(
                        evaluation
                );

                evaluations.add(
                        evaluation
                );
            }

            return evaluations.isEmpty()
                    ? null
                    : evaluations;

        } catch (Exception e) {

            System.err.println(
                    "[AIService] Batch evaluation JSON parsing failed."
            );

            System.err.println(
                    "[AIService] Failed to parse AI response: "
                            + e.getMessage()
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

            System.out.println(
                    "=== CLEAN QUESTION JSON ==="
            );

            System.out.println(
                    clean
            );

            System.out.println(
                    "=== END CLEAN QUESTION JSON ==="
            );

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
            System.err.println(
                    "[AIService] Failed to parse AI response: "
                            + e.getMessage()
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

        if (node != null &&
                node.isArray()) {

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

        /*
         * Gemini should now return JSON directly because
         * responseMimeType is application/json.
         *
         * We still keep this cleanup as a safety net in case
         * Markdown fences are returned.
         */

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

    private int requireIntField(
            JsonNode node,
            String fieldName) {

        JsonNode field =
                node.get(fieldName);

        if (field == null ||
                !field.isInt()) {

            throw new IllegalArgumentException(
                    "Missing or invalid integer field: "
                            + fieldName
            );
        }

        return field.asInt();
    }

    private String requireStringField(
            JsonNode node,
            String fieldName) {

        JsonNode field =
                node.get(fieldName);

        if (field == null ||
                !field.isTextual() ||
                field.asText().isBlank()) {

            throw new IllegalArgumentException(
                    "Missing or invalid string field: "
                            + fieldName
            );
        }

        return field.asText().trim();
    }

    private void validateEvaluation(
            AIEvaluation evaluation) {

        if (evaluation.score < 0 ||
                evaluation.score > 100) {

            throw new IllegalArgumentException(
                    "AI returned invalid score: "
                            + evaluation.score
            );
        }

        if (evaluation.fillerWords < 0) {

            throw new IllegalArgumentException(
                    "AI returned invalid filler word count: "
                            + evaluation.fillerWords
            );
        }

        if (evaluation.confidence < 0 ||
                evaluation.confidence > 100) {

            throw new IllegalArgumentException(
                    "AI returned invalid confidence: "
                            + evaluation.confidence
            );
        }

        String relevance =
                evaluation.relevance.toLowerCase();

        if (!List.of(
                "low",
                "medium",
                "high"
        ).contains(relevance)) {

            throw new IllegalArgumentException(
                    "AI returned invalid relevance: "
                            + evaluation.relevance
            );
        }

        String technicalAccuracy =
                evaluation.technicalAccuracy.toLowerCase();

        if (!List.of(
                "poor",
                "average",
                "good"
        ).contains(technicalAccuracy)) {

            throw new IllegalArgumentException(
                    "AI returned invalid technical accuracy: "
                            + evaluation.technicalAccuracy
            );
        }
    }
}