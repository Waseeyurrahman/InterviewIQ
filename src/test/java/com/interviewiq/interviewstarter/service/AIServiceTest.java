package com.interviewiq.interviewstarter.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class AIServiceTest {

    private final AIService aiService =
            new AIService();


    @Test
    void shouldRejectScoreBelowZero() throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.score = -1;

        assertValidationFails(
                evaluation,
                "invalid score"
        );
    }

    @Test
    void shouldRejectScoreAbove100() throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.score = 101;

        assertValidationFails(
                evaluation,
                "invalid score"
        );
    }

    @Test
    void shouldRejectConfidenceAbove100() throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.confidence = 101;

        assertValidationFails(
                evaluation,
                "invalid confidence"
        );
    }

    @Test
    void shouldRejectNegativeFillerWords() throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.fillerWords = -1;

        assertValidationFails(
                evaluation,
                "invalid filler"
        );
    }

    @Test
    void shouldRejectInvalidRelevance() throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.relevance = "invalid";

        assertValidationFails(
                evaluation,
                "invalid relevance"
        );
    }

    @Test
    void shouldRejectInvalidTechnicalAccuracy()
            throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        evaluation.technicalAccuracy = "invalid";

        assertValidationFails(
                evaluation,
                "invalid technical accuracy"
        );
    }

    @Test
    void shouldAcceptValidEvaluation()
            throws Exception {

        AIService.AIEvaluation evaluation =
                validEvaluation();

        assertDoesNotThrow(
                () -> invokeValidation(evaluation)
        );
    }

    private AIService.AIEvaluation validEvaluation() {

        AIService.AIEvaluation evaluation =
                new AIService.AIEvaluation();

        evaluation.score = 70;
        evaluation.fillerWords = 2;
        evaluation.confidence = 80;
        evaluation.relevance = "high";
        evaluation.technicalAccuracy = "good";

        evaluation.strengths =
                new ArrayList<>();

        evaluation.weaknesses =
                new ArrayList<>();

        evaluation.recommendations =
                new ArrayList<>();

        return evaluation;
    }

    private void invokeValidation(
            AIService.AIEvaluation evaluation)
            throws Exception {

        Method method =
                AIService.class.getDeclaredMethod(
                        "validateEvaluation",
                        AIService.AIEvaluation.class
                );

        method.setAccessible(true);

        method.invoke(
                aiService,
                evaluation
        );
    }

    private void assertValidationFails(
            AIService.AIEvaluation evaluation,
            String description)
            throws Exception {

        Exception exception =
                assertThrows(
                        Exception.class,
                        () -> invokeValidation(evaluation)
                );

        assertTrue(
                exception.getCause()
                        .getMessage()
                        .toLowerCase()
                        .contains(
                                description.split(" ")[1]
                        )
        );
    }
}