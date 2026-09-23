package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracticeCenterServiceImpl implements PracticeCenterService {

    private final PracticeCenterRepository centerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeCenter> listarCentros() {
        return centerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PracticeCenter> obtenerPorId(Long id) {
        return centerRepository.findById(id);
    }

    @Override
    @Transactional
    public PracticeCenter crearCentro(PracticeCenter center) {
        return centerRepository.save(center);
    }
}
