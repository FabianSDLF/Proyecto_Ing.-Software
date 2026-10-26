package com.ubb.practicas.service;

import com.ubb.practicas.model.PracticeCenter;

import java.util.List;
import java.util.Optional;

public interface PracticeCenterService {
    List<PracticeCenter> listarCentros();
    Optional<PracticeCenter> obtenerPorId(Long id);
    PracticeCenter crearCentro(PracticeCenter center);
    PracticeCenter actualizarCentro(Long id, PracticeCenter center);
    void eliminarCentro(Long id);
}
