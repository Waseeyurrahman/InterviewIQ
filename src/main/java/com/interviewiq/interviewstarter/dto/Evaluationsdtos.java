package com.interviewiq.interviewstarter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class Evaluationsdtos
{
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluateAnswersRequest{
        private List<AnswerDtos.SubmitAnswerItem> answers;
    }

    @Data
    @AllArgsConstructor
    public static class EvaluateAnswersResponse{
        private boolean success;
        private boolean aiAvailable;
        private int score;
        private int fillerWords;
        private int confidence;
        private String relevance;
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationItemResponse {

        private Long questionId;
        private String questionText;

        private int score;
        private int fillerWords;

        private String relevance;
        private String technicalAccuracy;

        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
    }
}
