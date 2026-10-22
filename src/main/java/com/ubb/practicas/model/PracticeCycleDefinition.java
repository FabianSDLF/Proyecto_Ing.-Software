package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.PracticeCycle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * HU5: Registro de los tres ciclos de práctica (Inicial, Intermedio, Avanzado).
 * <p>
 * Catálogo persistente con los datos descriptivos de cada ciclo. El {@link PracticeCycle}
 * sigue siendo la clave lógica que usan asignaturas y duplas; esta tabla agrega la
 * información que el Administrador puede consultar y editar (nombre, descripción,
 * semestres y orden). Existe exactamente un registro por cada valor del enum.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ciclos_practica")
public class PracticeCycleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, updatable = false, length = 50)
    private PracticeCycle code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer displayOrder;

    private Integer startSemester;

    private Integer endSemester;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
