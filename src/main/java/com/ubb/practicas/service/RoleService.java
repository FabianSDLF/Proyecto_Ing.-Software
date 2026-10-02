package com.ubb.practicas.service;

import com.ubb.practicas.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    List<Role> obtenerRoles();
    Optional<Role> buscarPorNombre(String name);
    Optional<Role> buscarPorId(Long id);
}
