package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Role> obtenerRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> buscarPorNombre(String name) {
        return Optional.ofNullable(roleRepository.findByName(name));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> buscarPorId(Long id) {
        return roleRepository.findById(id);
    }
}
