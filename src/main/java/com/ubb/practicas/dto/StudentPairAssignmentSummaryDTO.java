package com.ubb.practicas.dto;

import com.ubb.practicas.model.PracticeAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPairAssignmentSummaryDTO {

    private Long id;
    private Long assignmentId;

    private UserSummaryDTO student;
    private SubjectSummaryDTO subject;
    private CenterSummaryDTO center;
    private PeriodSummaryDTO period;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private String cycle;

    private Long centerId;
    private String centerName;

    private String status;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectSummaryDTO {
        private Long id;
        private String name;
        private String code;
        private String cycle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterSummaryDTO {
        private Long id;
        private String name;
        private String city;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodSummaryDTO {
        private Long id;
        private String name;
    }

    public static StudentPairAssignmentSummaryDTO fromAssignment(PracticeAssignment pa) {
        if (pa == null) return null;

        UserSummaryDTO studentDTO = null;
        if (pa.getStudent() != null) {
            studentDTO = UserSummaryDTO.builder()
                    .id(pa.getStudent().getId())
                    .fullName(pa.getStudent().getFullName())
                    .email(pa.getStudent().getEmail())
                    .build();
        }

        SubjectSummaryDTO subjectDTO = null;
        if (pa.getSubject() != null) {
            subjectDTO = SubjectSummaryDTO.builder()
                    .id(pa.getSubject().getId())
                    .name(pa.getSubject().getName())
                    .code(pa.getSubject().getCode())
                    .cycle(pa.getSubject().getCycle() != null ? pa.getSubject().getCycle().name() : null)
                    .build();
        }

        CenterSummaryDTO centerDTO = null;
        if (pa.getCenter() != null) {
            centerDTO = CenterSummaryDTO.builder()
                    .id(pa.getCenter().getId())
                    .name(pa.getCenter().getName())
                    .city(pa.getCenter().getCity())
                    .build();
        }

        PeriodSummaryDTO periodDTO = null;
        if (pa.getPeriod() != null) {
            periodDTO = PeriodSummaryDTO.builder()
                    .id(pa.getPeriod().getId())
                    .name(pa.getPeriod().getName())
                    .build();
        }

        return StudentPairAssignmentSummaryDTO.builder()
                .id(pa.getId())
                .assignmentId(pa.getId())
                .student(studentDTO)
                .subject(subjectDTO)
                .center(centerDTO)
                .period(periodDTO)
                .studentId(studentDTO != null ? studentDTO.getId() : null)
                .studentName(studentDTO != null ? studentDTO.getFullName() : null)
                .studentEmail(studentDTO != null ? studentDTO.getEmail() : null)
                .subjectId(subjectDTO != null ? subjectDTO.getId() : null)
                .subjectName(subjectDTO != null ? subjectDTO.getName() : null)
                .subjectCode(subjectDTO != null ? subjectDTO.getCode() : null)
                .cycle(subjectDTO != null ? subjectDTO.getCycle() : null)
                .centerId(centerDTO != null ? centerDTO.getId() : null)
                .centerName(centerDTO != null ? centerDTO.getName() : null)
                .status(pa.getStatus() != null ? pa.getStatus().name() : null)
                .build();
    }
}
