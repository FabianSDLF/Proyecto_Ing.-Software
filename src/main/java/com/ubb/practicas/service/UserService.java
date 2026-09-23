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
    void procesarArchivoMasivo(MultipartFile archivo) throws Exception;
    User actualizarRolUsuario(Long usuarioId, String roleName) throws Exception;
    User actualizarUsuario(Long usuarioId, User datosActualizados) throws Exception;
    User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception;
    User crearUsuario(User nuevoUsuario) throws Exception;
    User actualizarMiPerfil(String emailActual, UserProfileUpdateRequest request) throws Exception;
}