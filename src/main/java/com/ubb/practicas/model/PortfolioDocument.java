package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Documentos")
public class PortfolioDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "practice_assignment_id")
    private PracticeAssignment practiceAssignment;
    @Enumerated(EnumType.STRING)
    private DocumentType type;
    private String fileName;
    private String fileUrl;
    private String storedFileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadDate;
}
