package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.PracticeCycle;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Asignaturas")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)
    private PracticeCycle cycle;
    private String code;
}
