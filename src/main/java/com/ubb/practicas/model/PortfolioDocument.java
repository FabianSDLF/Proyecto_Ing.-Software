package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Documentos")
public class PortfolioDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practice_assignment_id")
    @ToString.Exclude
    private PracticeAssignment practiceAssignment;

    @Enumerated(EnumType.STRING)
    private DocumentType type;

    private String fileName;
    private String fileUrl;
    private String storedFileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadDate;

    @Column(length = 2000)
    private String teacherFeedback;
    private java.time.LocalDateTime feedbackDate;

    public PortfolioDocument(
            Long id,
            PracticeAssignment practiceAssignment,
            com.ubb.practicas.model.enums.DocumentType type,
            String fileName,
            String fileUrl,
            String storedFileName,
            String contentType,
            Long sizeBytes,
            java.time.LocalDateTime uploadDate) {
        this.id = id;
        this.practiceAssignment = practiceAssignment;
        this.type = type;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadDate = uploadDate;
    }

    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }

    public boolean isAccessibleBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.hasParticipant(email);
    }
}
