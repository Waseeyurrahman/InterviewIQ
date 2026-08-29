package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.dto.RecommendationDtos;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log =
            LoggerFactory.getLogger(RecommendationService.class);

    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    public RecommendationService(
            EvaluationRepository evaluationRepository,
            ObjectMapper objectMapper) {

        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
    }

    public RecommendationDtos.RecommendationResponse
    getRecommendations(Long userId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewUserId(userId);

        if (evaluations.isEmpty()) {

            return RecommendationDtos.RecommendationResponse.builder()
                    .recommendations(List.of())
                    .weakAreas(List.of())
                    .strengths(List.of())
                    .build();
        }

        List<String> recommendations =
                extractValues(
                        evaluations,
                        Evaluation::getRecommendations
                );

        List<String> weaknesses =
                extractValues(
                        evaluations,
                        Evaluation::getWeaknesses
                );

        List<String> strengths =
                extractValues(
                        evaluations,
                        Evaluation::getStrengths
                );

        return RecommendationDtos.RecommendationResponse.builder()

                .recommendations(
                        rank(recommendations)
                )

                .weakAreas(
                        rank(weaknesses)
                )

                .strengths(
                        rank(strengths)
                )

                .build();
    }


    private List<String> extractValues(
            List<Evaluation> evaluations,
            java.util.function.Function<Evaluation, String> extractor) {

        return evaluations.stream()

                .map(extractor)

                .filter(Objects::nonNull)

                .filter(value -> !value.isBlank())

                .flatMap(value -> parseJsonList(value).stream())

                .map(String::trim)

                .filter(value -> !value.isBlank())

                .toList();
    }


    private List<String> parseJsonList(String value) {

        try {

            return objectMapper.readValue(
                    value,
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception e) {

            log.warn(
                    "Could not parse recommendation data: {}",
                    e.getMessage()
            );

            return List.of();
        }
    }


    private List<RecommendationDtos.RecommendationItem>
    rank(List<String> values) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String value : values) {

            String normalized = value.trim();

            String existingKey = counts.keySet()
                    .stream()
                    .filter(key ->
                            key.equalsIgnoreCase(normalized)
                    )
                    .findFirst()
                    .orElse(null);

            if (existingKey == null) {
                counts.put(normalized, 1);
            } else {
                counts.put(
                        existingKey,
                        counts.get(existingKey) + 1
                );
            }
        }

        return counts.entrySet()
                .stream()
                .sorted(
                        Map.Entry.<String, Integer>
                                        comparingByValue()
                                .reversed()
                )
                .limit(10)
                .map(entry ->
                        RecommendationDtos.RecommendationItem
                                .builder()
                                .text(entry.getKey())
                                .count(entry.getValue())
                                .build()
                )
                .toList();
    }
}