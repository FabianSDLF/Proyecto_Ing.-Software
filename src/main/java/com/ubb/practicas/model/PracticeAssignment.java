package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Asignacion_Practicas")
public class PracticeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "center_id")
    private PracticeCenter center;

    @ManyToOne
    @JoinColumn(name = "collaborator_id")
    private User collaborator;

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private User tutor;

    private String semester;
}