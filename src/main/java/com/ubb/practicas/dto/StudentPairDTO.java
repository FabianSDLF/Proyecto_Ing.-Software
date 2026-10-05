package com.ubb.practicas.dto;

import com.ubb.practicas.model.StudentPair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPairDTO {

    private Long id;
    private Long periodId;
    private String periodName;
    private StudentPairAssignmentSummaryDTO.PeriodSummaryDTO period;

    private Long assignment1Id;
    private Long student1Id;
    private String student1Name;
    private String student1Email;

    private Long assignment2Id;
    private Long student2Id;
    private String student2Name;
    private String student2Email;

    private Long subjectId;
    private String subjectName;
    private String cycle;
    private String centerName;
    private LocalDateTime createdAt;

    private StudentPairAssignmentSummaryDTO assignment1;
    private StudentPairAssignmentSummaryDTO assignment2;

    public static StudentPairDTO fromEntity(StudentPair pair) {
        if (pair == null) return null;

        StudentPairAssignmentSummaryDTO a1 = StudentPairAssignmentSummaryDTO.fromAssignment(pair.getAssignment1());
        StudentPairAssignmentSummaryDTO a2 = StudentPairAssignmentSummaryDTO.fromAssignment(pair.getAssignment2());

        String resolvedCenter = null;
        if (a1 != null && a1.getCenterName() != null) {
            resolvedCenter = a1.getCenterName();
        } else if (a2 != null && a2.getCenterName() != null) {
            resolvedCenter = a2.getCenterName();
        }

        String resolvedSubject = null;
        Long resolvedSubjectId = null;
        String resolvedCycle = null;
        if (a1 != null && a1.getSubjectName() != null) {
            resolvedSubject = a1.getSubjectName();
            resolvedSubjectId = a1.getSubjectId();
            resolvedCycle = a1.getCycle();
        } else if (a2 != null && a2.getSubjectName() != null) {
            resolvedSubject = a2.getSubjectName();
            resolvedSubjectId = a2.getSubjectId();
            resolvedCycle = a2.getCycle();
        }

        StudentPairAssignmentSummaryDTO.PeriodSummaryDTO periodDTO = null;
        if (pair.getPeriod() != null) {
            periodDTO = StudentPairAssignmentSummaryDTO.PeriodSummaryDTO.builder()
                    .id(pair.getPeriod().getId())
                    .name(pair.getPeriod().getName())
                    .build();
        }

        return StudentPairDTO.builder()
                .id(pair.getId())
                .periodId(pair.getPeriod() != null ? pair.getPeriod().getId() : null)
                .periodName(pair.getPeriod() != null ? pair.getPeriod().getName() : null)
                .period(periodDTO)
                .assignment1Id(a1 != null ? a1.getId() : null)
                .student1Id(a1 != null ? a1.getStudentId() : null)
                .student1Name(a1 != null ? a1.getStudentName() : null)
                .student1Email(a1 != null ? a1.getStudentEmail() : null)
                .assignment2Id(a2 != null ? a2.getId() : null)
                .student2Id(a2 != null ? a2.getStudentId() : null)
                .student2Name(a2 != null ? a2.getStudentName() : null)
                .student2Email(a2 != null ? a2.getStudentEmail() : null)
                .subjectId(resolvedSubjectId)
                .subjectName(resolvedSubject)
                .cycle(resolvedCycle != null ? resolvedCycle : "INTERMEDIO")
                .centerName(resolvedCenter)
                .createdAt(pair.getCreatedAt())
                .assignment1(a1)
                .assignment2(a2)
                .build();
    }
}
