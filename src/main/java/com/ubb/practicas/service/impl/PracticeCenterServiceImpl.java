package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.model.enums.SchoolDependency;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.PracticeCenterRepository;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PracticeCenterServiceImpl implements PracticeCenterService {

    private final PracticeCenterRepository centerRepository;
    private final PracticeAssignmentRepository assignmentRepository;

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
        if (center.getName() == null || center.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del centro de práctica es obligatorio");
        }
        center.setId(null);
        center.setName(center.getName().trim());
        if (center.getDependency() == null) {
            center.setDependency(SchoolDependency.PUBLICO_SLEP);
        }
        return centerRepository.save(center);
    }

    @Override
    @Transactional
    public PracticeCenter actualizarCentro(Long id, PracticeCenter datos) {
        PracticeCenter existing = centerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Centro de práctica no encontrado"));

        if (datos.getName() != null && !datos.getName().trim().isEmpty()) {
            existing.setName(datos.getName().trim());
        }
        if (datos.getDependency() != null) {
            existing.setDependency(datos.getDependency());
        }
        existing.setAddress(datos.getAddress());
        existing.setCity(datos.getCity());
        existing.setDistrict(datos.getDistrict());
        existing.setPhone(datos.getPhone());
        existing.setContactName(datos.getContactName());
        existing.setContactEmail(datos.getContactEmail());

        return centerRepository.save(existing);
    }

    @Override
    @Transactional
    public void eliminarCentro(Long id) {
        if (!centerRepository.existsById(id)) {
            throw new NoSuchElementException("Centro de práctica no encontrado");
        }
        if (assignmentRepository != null && assignmentRepository.existsByCenterId(id)) {
            throw new IllegalStateException("No se puede eliminar el centro de práctica porque tiene estudiantes asignados.");
        }
        try {
            centerRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar el centro de práctica porque tiene registros asociados.", e);
        }
    }
}
