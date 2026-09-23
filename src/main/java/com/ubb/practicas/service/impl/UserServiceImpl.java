package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserBatchImportService;
import com.ubb.practicas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserBatchImportService userBatchImportService;

    @Override
    @Transactional(readOnly = true)
    public List<User> obtenerTodosUsuarios() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listarUsuariosPorRol(String roleName) {
        return userRepository.findByRoleName(roleName);
    }

    @Override
    @Transactional(readOnly = true)
    public User obtenerPorEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public void procesarArchivoMasivo(MultipartFile archivo) throws Exception {
        userBatchImportService.procesarArchivoMasivo(archivo);
    }

    @Override
    @Transactional
    public User actualizarRolUsuario(Long usuarioId, String roleName) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new Exception("Rol no encontrado: " + roleName);
        }

        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User actualizarUsuario(Long usuarioId, User datosActualizados) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));

        if (datosActualizados.getFullName() != null) {
            user.setFullName(datosActualizados.getFullName());
        }
        if (datosActualizados.getEmail() != null && !user.getEmail().equals(datosActualizados.getEmail())) {
            if (userRepository.findByEmail(datosActualizados.getEmail()) != null) {
                throw new Exception("El email ya está en uso");
            }
            user.setEmail(datosActualizados.getEmail());
        }
        if (datosActualizados.getRole() != null && datosActualizados.getRole().getName() != null) {
            Role role = roleRepository.findByName(datosActualizados.getRole().getName());
            if (role != null) {
                user.setRole(role);
            }
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        user.setActive(estado);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User crearUsuario(User nuevoUsuario) throws Exception {
        if (userRepository.findByEmail(nuevoUsuario.getEmail()) != null) {
            throw new Exception("El email ya está en uso");
        }

        if (nuevoUsuario.getRole() != null && nuevoUsuario.getRole().getName() != null) {
            Role role = roleRepository.findByName(nuevoUsuario.getRole().getName());
            if (role == null) {
                throw new Exception("Rol no encontrado: " + nuevoUsuario.getRole().getName());
            }
            nuevoUsuario.setRole(role);
        }

        if (nuevoUsuario.getPasswordHash() != null) {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(nuevoUsuario.getPasswordHash()));
        } else {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode("temporal123"));
        }

        nuevoUsuario.setCreatedAt(LocalDateTime.now());
        nuevoUsuario.setActive(true);

        return userRepository.save(nuevoUsuario);
    }

    @Override
    @Transactional
    public User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) throws Exception {
        User user = userRepository.findByEmail(emailActual);
        if (user == null) {
            throw new Exception("Usuario no encontrado");
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()) != null) {
                throw new Exception("El email ya está en uso por otro usuario");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return userRepository.save(user);
    }
}
