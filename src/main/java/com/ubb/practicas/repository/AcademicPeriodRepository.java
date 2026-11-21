package com.ubb.practicas.repository;

import com.ubb.practicas.model.AcademicPeriod;
import com.ubb.practicas.model.enums.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {
    List<AcademicPeriod> findByStatus(PeriodStatus status);
}
