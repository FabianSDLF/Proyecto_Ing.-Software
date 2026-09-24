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

    public boolean isOwnedBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.isStudent(email);
    }

    public boolean isAccessibleBy(String email) {
        return email != null && this.practiceAssignment != null && this.practiceAssignment.hasParticipant(email);
    }
}
