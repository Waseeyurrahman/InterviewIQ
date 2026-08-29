package com.interviewiq.interviewstarter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AnswerDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerRequest{
        private Long questionId;
        private String answerText;
    }


    @Data
    @AllArgsConstructor
    public static class AnswerResponse{
        private boolean success;
        private String message;
        private Long answerId;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswerItem {

        @NotNull(message = "Question ID is required")
        private Long questionId;

        @Size(max = 10000, message = "Question text must not exceed 10000 characters")
        private String questionText;

        @NotBlank(message = "Answer text is required")
        @Size(max = 10000, message = "Answer text must not exceed 10000 characters")
        private String answerText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitAnswersRequest {

        @NotEmpty(message = "At least one answer is required")
        private List<@Valid SubmitAnswerItem> answers;
    }

    @Data
    @AllArgsConstructor
    public static class SubmitAnswersResponse {
        private boolean success;
        private int saved;
    }


}
