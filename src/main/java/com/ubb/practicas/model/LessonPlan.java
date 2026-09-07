package com.ubb.practicas.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Planificaciones")
public class LessonPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;

    @Column(length = 2000)
    private String objectives;

    @Column(length = 2000)
    private String teacherFeedback;

    // TODO: Agrega la anotación @OneToOne
    // TODO: Agrega @JoinColumn(name = "document_id")
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "document_id")
    private PortfolioDocument document;
}