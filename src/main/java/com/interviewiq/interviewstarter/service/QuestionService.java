package com.interviewiq.interviewstarter.service;

import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.Question;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private static final int DEFAULT_QUESTION_COUNT = 5;

    private static final List<String> FALLBACK_QUESTIONS = List.of(
            "Tell me about yourself and your background.",
            "Describe a challenging project you worked on recently.",
            "What are your biggest strengths and weaknesses?",
            "Why are you interested in this role?",
            "Where do you see yourself in 5 years?",
            "Explain a difficult technical problem you solved.",
            "How do you handle tight deadlines and pressure?",
            "How do you approach debugging a difficult issue?",
            "Describe a situation where you had to learn something quickly.",
            "Where do you see yourself growing professionally?"
    );

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final AIService aiService;

    public QuestionService(
            QuestionRepository questionRepository,
            InterviewRepository interviewRepository,
            AIService aiService) {

        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.aiService = aiService;
    }

    public List<Question> getQuestionsForInterview(Long interviewId) {

        // 1. Return cached questions if already generated
        List<Question> existing =
                questionRepository.findByInterviewIdOrderByIdAsc(interviewId);

        if (!existing.isEmpty()) {
            return existing;
        }

        // 2. Find interview
        Optional<Interview> opt =
                interviewRepository.findById(interviewId);

        if (opt.isEmpty()) {
            return List.of();
        }

        Interview interview = opt.get();

        // 3. Determine question count from duration
        int questionCount =
                getQuestionCount(interview.getDuration());

        System.out.println(
                "[QuestionService] Duration = "
                        + interview.getDuration()
                        + " minutes, generating "
                        + questionCount
                        + " questions"
        );

        // 4. Generate questions using AI
        List<String> texts =
                aiService.generateQuestions(
                        interview.getRole(),
                        interview.getExperienceLevel(),
                        interview.getDifficulty(),
                        questionCount
                );

        // 5. Fallback if AI fails
        if (texts == null || texts.isEmpty()) {

            System.out.println(
                    "[QuestionService] AI unavailable - using fallback questions."
            );

            texts = FALLBACK_QUESTIONS
                    .stream()
                    .limit(questionCount)
                    .toList();
        }

        if (texts.size() > questionCount) {
            texts = new ArrayList<>(
                    texts.subList(0, questionCount)
            );
        }

        // 7. Persist questions
        List<Question> toSave = new ArrayList<>();

        for (String text : texts) {

            toSave.add(
                    new Question(
                            null,
                            interview,
                            text
                    )
            );
        }

        return questionRepository.saveAll(toSave);
    }

    /**
     * Determines the number of questions
     * based on interview duration.
     */
    private int getQuestionCount(Integer duration) {

        if (duration == null) {
            return DEFAULT_QUESTION_COUNT;
        }

        return switch (duration) {
            case 10 -> 5;
            case 20 -> 7;
            case 30 -> 10;
            default -> DEFAULT_QUESTION_COUNT;
        };
    }
}