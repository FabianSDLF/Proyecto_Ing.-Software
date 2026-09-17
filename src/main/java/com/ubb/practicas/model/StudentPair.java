package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Duplas")
public class StudentPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "assignment1_id")
    private PracticeAssignment assignment1;
    @ManyToOne
    @JoinColumn(name = "assignment2_id")
    private PracticeAssignment assignment2;
    @ManyToOne
    @JoinColumn(name = "period_id")
    private AcademicPeriod period;
}
