package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.SchoolDependency;
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
@Table(name = "Centro_Practicas")
public class PracticeCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private SchoolDependency dependency;

    private String address;
    private String city;
    private String district;
    private String phone;
    private String contactName;
    private String contactEmail;
}
