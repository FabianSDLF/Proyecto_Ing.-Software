package com.ubb.practicas.dto;

import com.ubb.practicas.model.enums.AchievementLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCriteriaDTO {
    private Long id;
    private String criterionCode;
    private String dimension;
    private Integer order;
    private String text;
    private String criteriaName;
    private AchievementLevel achievementLevel;
    private Float points;
    private String justification;
}
