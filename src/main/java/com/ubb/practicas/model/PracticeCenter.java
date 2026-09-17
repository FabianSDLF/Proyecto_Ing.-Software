package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.SchoolDependency;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Centro_Practicas")
public class PracticeCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
