package com.ubb.practicas.dto;

import com.ubb.practicas.model.enums.AchievementLevel;
import lombok.Data;

@Data
public class EvaluationCriteriaDTO {
    private String criteriaName;
    private AchievementLevel achievementLevel;
    private Float points;
    private String justification;
}
