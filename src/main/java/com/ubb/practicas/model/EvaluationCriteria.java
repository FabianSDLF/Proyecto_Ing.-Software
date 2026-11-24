package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.AchievementLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Criterios_Evaluacion")
public class EvaluationCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id")
    @ToString.Exclude
    private Evaluation evaluation;

    @Column(name = "criterion_code", length = 50)
    private String criterionCode;

    @Column(name = "dimension", length = 50)
    private String dimension;

    @Column(name = "display_order")
    private Integer displayOrder;

    private String criteriaName;

    @Enumerated(EnumType.STRING)
    private AchievementLevel achievementLevel;

    private Float points;

    @Column(length = 1000)
    private String justification;

    public EvaluationCriteria(
            Long id,
            Evaluation evaluation,
            String criteriaName,
            AchievementLevel achievementLevel,
            Float points,
            String justification
    ) {
        this.id = id;
        this.evaluation = evaluation;
        this.criteriaName = criteriaName;
        this.achievementLevel = achievementLevel;
        this.points = points;
        this.justification = justification;
    }
}
