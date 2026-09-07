package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

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

    private String type;

    private String fileUrl;

    private Date uploadDate;
}