package com.interviewiq.interviewstarter.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.entity.Evaluation;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.repository.AnswerRepository;
import com.interviewiq.interviewstarter.repository.EvaluationRepository;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final Logger log =
            LoggerFactory.getLogger(DashboardService.class);

    private final InterviewRepository interviewRepository;
    private final EvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    public DashboardService(InterviewRepository interviewRepository,EvaluationRepository evaluationRepository,ObjectMapper objectMapper){
        this.interviewRepository = interviewRepository;
        this.evaluationRepository = evaluationRepository;
        this.objectMapper = objectMapper;
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardResponse buildDashboard(Long userId)
    {
        List<Interview> finished = interviewRepository
                .findByUserId(userId)
                .stream()
                .filter(interview -> interview.getFinalScore() != null)
                .toList();
        if(finished.isEmpty()){
            return emptyDashboard();
        }

        int average = (int) Math.round(finished.stream().mapToInt(Interview::getFinalScore).average().orElse(0));
        int best = finished.stream().mapToInt(Interview::getFinalScore).max().orElse(0);
        int totalMinutes = finished.stream().mapToInt(iv ->iv.getDuration()==null? 0: iv.getDuration()).sum();

        return DashboardResponse.builder()
                .totalInterviews(finished.size())
                .averageScore(average)
                .totalPracticeTime(formatMin(totalMinutes))
                .bestScore(best)
                .scoreTrend(buildingTrend(finished))
                .weakAreas(buildWeakAreas(userId))
                .recentInterviews(buildRecent(finished))
                .strengths(buildStrengths(userId))
                .build();
    }

    private String formatMin(int totalMinutes){
        if(totalMinutes<=0){
            return "0m";
        }
        int h = totalMinutes/60, m = totalMinutes%60;
        return h==0? m+"m":h+"h"+m+"m";
    }
    private List<DashboardResponse.TrendPoint> buildingTrend(
            List<Interview> finished) {

        Map<LocalDate, List<Integer>> byDay =
                finished.stream()
                        .filter(iv -> iv.getCompletedAt() != null)
                        .collect(Collectors.groupingBy(
                                iv -> iv.getCompletedAt().toLocalDate(),
                                Collectors.mapping(
                                        Interview::getFinalScore,
                                        Collectors.toList()
                                )
                        ));

        return byDay.entrySet()
                .stream()
                .map(entry -> {

                    LocalDate date = entry.getKey();

                    int averageScore =
                            (int) Math.round(
                                    entry.getValue()
                                            .stream()
                                            .mapToInt(Integer::intValue)
                                            .average()
                                            .orElse(0)
                            );

                    return new DashboardResponse.TrendPoint(
                            date.format(DATE_FMT),
                            averageScore
                    );
                })
                .sorted(
                        Comparator.comparing(
                                DashboardResponse.TrendPoint::getDate
                        )
                )
                .toList();
    }


    private List<DashboardResponse.Weakness> buildWeakAreas(Long userId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewUserId(userId);

        return evaluations.stream()
                .map(Evaluation::getWeaknesses)
                .filter(Objects::nonNull)
                .filter(weaknesses -> !weaknesses.isBlank())
                .flatMap(weaknesses -> parseWeaknesses(weaknesses, objectMapper))
                .filter(Objects::nonNull)
                .filter(weakness -> !weakness.isBlank())
                .distinct()
                .limit(5)
                .map(DashboardResponse.Weakness::new)
                .toList();
    }
    private Stream<String> parseWeaknesses(
            String weaknesses,
            ObjectMapper objectMapper) {

        try {

            List<String> weaknessList =
                    objectMapper.readValue(
                            weaknesses,
                            new TypeReference<List<String>>() {}
                    );

            return weaknessList.stream();

        } catch (Exception e) {

            log.warn(
                    "Failed to parse stored weakness data: {}",
                    e.getMessage()
            );

            return Stream.empty();
        }
    }




    private List<DashboardResponse.RecentInterviews> buildRecent(
            List<Interview> finished) {

        return finished.stream()
                .sorted(
                        Comparator.comparingLong(
                                Interview::getId
                        ).reversed()
                )
                .limit(5)
                .map(iv -> {

                    int score =
                            iv.getFinalScore();

                    String status =
                            score >= 70
                                    ? "Completed"
                                    : score >= 40
                                    ? "Needs Review"
                                    : "Practiced";

                    LocalDate when =
                            iv.getCompletedAt() != null
                                    ? iv.getCompletedAt().toLocalDate()
                                    : LocalDate.now();

                    return DashboardResponse.RecentInterviews
                            .builder()

                            .interviewId(
                                    iv.getId()
                            )

                            .role(
                                    iv.getRole() == null
                                            ? "General Role"
                                            : iv.getRole()
                            )

                            .experienceLevel(
                                    iv.getExperienceLevel() == null
                                            ? "Not specified"
                                            : iv.getExperienceLevel()
                            )

                            .difficulty(
                                    iv.getDifficulty() == null
                                            ? "Not specified"
                                            : iv.getDifficulty()
                            )

                            .date(
                                    when.format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd"
                                            )
                                    )
                            )

                            .score(score)

                            .status(status)

                            .build();
                })
                .toList();
    }


    private List<DashboardResponse.NamedValue> buildStrengths(Long userId) {

        List<Evaluation> evaluations =
                evaluationRepository
                        .findByAnswerQuestionInterviewUserId(userId);

        if (evaluations.isEmpty()) {
            return List.of();
        }

        // Technical Depth = average evaluation score
        int technicalDepth = (int) Math.round(
                evaluations.stream()
                        .mapToInt(Evaluation::getScore)
                        .average()
                        .orElse(0)
        );

        // Relevance = average only of evaluations
        // where relevance was actually available
        double relevanceAverage =
                evaluations.stream()
                        .mapToInt(e -> relevanceScore(e.getRelevance()))
                        .filter(score -> score >= 0)
                        .average()
                        .orElse(0);

        int relevance = (int) Math.round(relevanceAverage);

        // Technical Accuracy = average only where available
        double accuracyAverage =
                evaluations.stream()
                        .mapToInt(e ->
                                technicalAccuracyScore(
                                        e.getTechnicalAccuracy()
                                )
                        )
                        .filter(score -> score >= 0)
                        .average()
                        .orElse(0);

        int technicalAccuracy = (int) Math.round(accuracyAverage);

        // Communication
        // Start from evaluation score and apply a small filler-word penalty.
        double communicationAverage =
                evaluations.stream()
                        .mapToInt(e -> {

                            int score = e.getScore();

                            int fillerWords =
                                    e.getFillerWords() == null
                                            ? 0
                                            : e.getFillerWords();

                            return Math.max(
                                    0,
                                    score - (fillerWords * 2)
                            );
                        })
                        .average()
                        .orElse(0);

        int communication = (int) Math.round(communicationAverage);

        return List.of(

                new DashboardResponse.NamedValue(
                        "Technical Depth",
                        clamp(technicalDepth)
                ),

                new DashboardResponse.NamedValue(
                        "Technical Accuracy",
                        clamp(technicalAccuracy)
                ),

                new DashboardResponse.NamedValue(
                        "Relevance",
                        clamp(relevance)
                ),

                new DashboardResponse.NamedValue(
                        "Communication",
                        clamp(communication)
                )
        );
    }

    private int relevanceScore(String relevance) {

        if (relevance == null || relevance.isBlank()) {
            return -1; // ignore missing value
        }

        return switch (relevance.toLowerCase().trim()) {

            case "high", "excellent", "strong" -> 100;

            case "medium", "moderate", "good" -> 75;

            case "low", "poor", "weak" -> 40;

            default -> -1;
        };
    }

    private int technicalAccuracyScore(String technicalAccuracy) {

        if (technicalAccuracy == null || technicalAccuracy.isBlank()) {
            return -1; // ignore missing value
        }

        return switch (technicalAccuracy.toLowerCase().trim()) {

            case "high", "excellent", "strong", "accurate" -> 100;

            case "medium", "moderate", "good" -> 75;

            case "low", "poor", "weak", "inaccurate" -> 40;

            default -> -1;
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private DashboardResponse emptyDashboard() {
        return DashboardResponse.builder()
                .totalInterviews(0)
                .averageScore(0)
                .bestScore(0)
                .totalPracticeTime("0m")
                .scoreTrend(List.of())
                .weakAreas(List.of())
                .recentInterviews(List.of())
                .strengths(List.of())
                .build();
    }
}
