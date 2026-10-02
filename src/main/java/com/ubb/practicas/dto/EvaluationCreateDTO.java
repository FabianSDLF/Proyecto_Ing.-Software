package com.ubb.practicas.dto;

import com.ubb.practicas.model.enums.AchievementLevel;
import com.ubb.practicas.model.enums.EvaluationType;
import lombok.Data;
import java.util.List;

@Data
public class EvaluationCreateDTO {
    private Long practiceAssignmentId;
    private EvaluationType type;
    private Float score;
    private String qualitativeComments;
    private List<EvaluationCriteriaDTO> criteria;
}
