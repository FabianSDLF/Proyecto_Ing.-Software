package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    private Float score;

    @Column(length = 1000)
    private String comments;

}