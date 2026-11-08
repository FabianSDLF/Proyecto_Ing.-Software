package com.ubb.practicas.service;

import com.ubb.practicas.model.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> listarAsignaturas();
    Subject crearAsignatura(Subject subject);
    Subject actualizarAsignatura(Long id, Subject subject);
    void eliminarAsignatura(Long id);
}
