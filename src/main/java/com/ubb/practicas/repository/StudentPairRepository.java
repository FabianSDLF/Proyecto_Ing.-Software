package com.ubb.practicas.repository;

import com.ubb.practicas.model.StudentPair;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPairRepository extends JpaRepository<StudentPair, Long> {
}
