package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Duplas")
public class StudentPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment1_id")
    @ToString.Exclude
    private PracticeAssignment assignment1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment2_id")
    @ToString.Exclude
    private PracticeAssignment assignment2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    @ToString.Exclude
    private AcademicPeriod period;
}
