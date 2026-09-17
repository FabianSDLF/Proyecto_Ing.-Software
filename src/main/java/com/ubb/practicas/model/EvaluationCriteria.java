package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.AchievementLevel;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Criterios_Evaluacion")
public class EvaluationCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation;
    private String criteriaName;
    @Enumerated(EnumType.STRING)
    private AchievementLevel achievementLevel;
    private Float points;
    @Column(length = 1000)
    private String justification;
}
