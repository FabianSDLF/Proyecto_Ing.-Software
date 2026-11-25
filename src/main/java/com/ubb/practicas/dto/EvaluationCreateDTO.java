package com.ubb.practicas.dto;

import com.ubb.practicas.model.enums.EvaluationType;
import lombok.Data;
import java.util.List;

@Data
public class EvaluationCreateDTO {
    private Long practiceAssignmentId;
    private EvaluationType type;
    private String rubricVersion;
    private Long expectedRevision;
    private Float score;
    private String qualitativeComments;
    private Boolean isFinalized;
    private String digitalSignature;
    private List<EvaluationCriteriaDTO> criteria;
}
