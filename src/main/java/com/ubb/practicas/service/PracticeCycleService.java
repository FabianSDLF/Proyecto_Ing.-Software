package com.ubb.practicas.service;

import com.ubb.practicas.dto.PracticeCycleDTO;
import com.ubb.practicas.dto.PracticeCycleUpdateRequest;
import com.ubb.practicas.model.enums.PracticeCycle;

import java.util.List;

/**
 * HU5: Registro y consulta de los tres ciclos de práctica.
 */
public interface PracticeCycleService {

    /** Registra los ciclos que falten (idempotente). Devuelve cuántos se crearon. */
    int registrarCiclosFaltantes();

    List<PracticeCycleDTO> listarCiclos();

    PracticeCycleDTO obtenerCiclo(PracticeCycle code);

    PracticeCycleDTO actualizarCiclo(PracticeCycle code, PracticeCycleUpdateRequest request);
}
