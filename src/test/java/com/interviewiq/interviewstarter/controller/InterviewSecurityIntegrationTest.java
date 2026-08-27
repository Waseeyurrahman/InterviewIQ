package com.interviewiq.interviewstarter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewiq.interviewstarter.entity.Interview;
import com.interviewiq.interviewstarter.entity.InterviewStatus;
import com.interviewiq.interviewstarter.entity.User;
import com.interviewiq.interviewstarter.repository.InterviewRepository;
import com.interviewiq.interviewstarter.repository.UserRepository;
import com.interviewiq.interviewstarter.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {

        userA = new User();
        userA.setName("User A");
        userA.setEmail("usera@test.com");
        userA.setPassword(
                passwordEncoder.encode("password123")
        );

        userA = userRepository.save(userA);

        userB = new User();
        userB.setName("User B");
        userB.setEmail("userb@test.com");
        userB.setPassword(
                passwordEncoder.encode("password123")
        );

        userB = userRepository.save(userB);
    }

    @Test
    void userCannotStartAnotherUsersInterview()
            throws Exception {

        Interview interview = new Interview();

        interview.setRole("Java Developer");
        interview.setExperienceLevel("Mid Level");
        interview.setDifficulty("Medium");
        interview.setDuration(30);
        interview.setStatus(InterviewStatus.CREATED);
        interview.setUser(userA);

        interview = interviewRepository.save(interview);

        String token = jwtService.generateToken(
                userB.getEmail()
        );

        mockMvc.perform(
                        post("/interview/{id}/start", interview.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .with(csrf())
                )
                .andExpect(status().isForbidden());
    }
    @Test
    void unauthenticatedUserCannotStartInterview()
            throws Exception {

        mockMvc.perform(
                        post("/interview/999/start")
                )
                .andExpect(status().isUnauthorized());
    }
}