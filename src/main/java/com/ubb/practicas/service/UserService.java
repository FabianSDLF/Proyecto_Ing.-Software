package com.ubb.practicas.service;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Servicio de dominio responsable del ciclo de vida y gestión de usuarios (SRP - BK-29).
 */
public interface UserService {
    List<User> obtenerTodosUsuarios();
    List<User> listarUsuariosPorRol(String roleName);
    User obtenerPorEmail(String email);
    void procesarArchivoMasivo(MultipartFile archivo);
    User actualizarRolUsuario(Long usuarioId, String roleName);
    User actualizarUsuario(Long usuarioId, User datosActualizados);
    User cambiarEstadoUsuario(Long usuarioId, boolean estado);
    User crearUsuario(User nuevoUsuario);
    User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request);
}