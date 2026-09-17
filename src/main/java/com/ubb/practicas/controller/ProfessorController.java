package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/profesor")
public class ProfessorController {

    @Autowired
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @GetMapping("/{professorId}/estudiantes")
    public List<PracticeAssignment> obtenerEstudiantesPorProfesor(@PathVariable Long professorId) {
        return practiceAssignmentRepository.findBySubjectProfessorId(professorId);
    }
}
