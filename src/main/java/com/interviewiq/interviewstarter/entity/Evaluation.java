package com.interviewiq.interviewstarter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "answer_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_evaluation_answer")
    )
    @JsonIgnore
    private Answer answer;

    @Column(nullable = false)
    private Integer score;

    private Integer fillerWords;

    private String relevance;

    private String technicalAccuracy;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String recommendations;
}