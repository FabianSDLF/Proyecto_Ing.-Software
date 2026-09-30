package com.ubb.practicas.service;

import com.ubb.practicas.dto.UserProfileUpdateRequest;
import com.ubb.practicas.exception.EmailAlreadyInUseException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.exception.RoleNotFoundException;
import com.ubb.practicas.exception.UserNotFoundException;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserBatchImportService userBatchImportService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void crearUsuario_Success_WithPasswordHashing() {
        Role role = new Role(1L, "Tutor");
        User user = new User();
        user.setEmail("tutor@ubb.cl");
        user.setFullName("Profesor Tutor");
        user.setPasswordHash("rawSecret123");
        user.setRole(new Role(null, "Tutor"));

        when(userRepository.findByEmail("tutor@ubb.cl")).thenReturn(null);
        when(roleRepository.findByName("Tutor")).thenReturn(role);
        when(passwordEncoder.encode("rawSecret123")).thenReturn("hashedSecret123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.crearUsuario(user);

        assertNotNull(result);
        assertEquals("hashedSecret123", result.getPasswordHash());
        assertTrue(result.getActive());
        assertNotNull(result.getCreatedAt());
        assertEquals(role, result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void crearUsuario_NullPassword_UsesDefaultTemporalPassword() {
        User user = new User();
        user.setEmail("temp@ubb.cl");
        user.setPasswordHash(null);

        when(userRepository.findByEmail("temp@ubb.cl")).thenReturn(null);
        when(passwordEncoder.encode("temporal123")).thenReturn("hashedTemporal");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.crearUsuario(user);

        assertNotNull(result);
        assertEquals("hashedTemporal", result.getPasswordHash());
        verify(passwordEncoder).encode("temporal123");
    }

    @Test
    void crearUsuario_DuplicateEmail_ThrowsResourceConflictException() {
        User user = new User();
        user.setEmail("existing@ubb.cl");

        when(userRepository.findByEmail("existing@ubb.cl")).thenReturn(new User());

        assertThrows(EmailAlreadyInUseException.class, () -> userService.crearUsuario(user));
        assertThrows(ResourceConflictException.class, () -> userService.crearUsuario(user));

        verify(userRepository, never()).save(any());
    }

    @Test
    void crearUsuario_RoleNotFound_ThrowsRoleNotFoundException() {
        User user = new User();
        user.setEmail("user@ubb.cl");
        user.setRole(new Role(null, "NonExistentRole"));

        when(userRepository.findByEmail("user@ubb.cl")).thenReturn(null);
        when(roleRepository.findByName("NonExistentRole")).thenReturn(null);

        assertThrows(RoleNotFoundException.class, () -> userService.crearUsuario(user));
        verify(userRepository, never()).save(any());
    }

    @Test
    void actualizarRolUsuario_Success() {
        User user = new User();
        user.setId(1L);
        user.setRole(new Role(1L, "Estudiante"));

        Role newRole = new Role(2L, "Tutor");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("Tutor")).thenReturn(newRole);
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.actualizarRolUsuario(1L, "Tutor");

        assertEquals(newRole, result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void actualizarRolUsuario_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.actualizarRolUsuario(99L, "Tutor"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void actualizarRolUsuario_RoleNotFound_ThrowsException() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("FakeRole")).thenReturn(null);

        assertThrows(RoleNotFoundException.class, () -> userService.actualizarRolUsuario(1L, "FakeRole"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cambiarEstadoUsuario_Deactivate_Success() {
        User user = new User();
        user.setId(1L);
        user.setActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.cambiarEstadoUsuario(1L, false);

        assertFalse(result.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void cambiarEstadoUsuario_Activate_Success() {
        User user = new User();
        user.setId(1L);
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.cambiarEstadoUsuario(1L, true);

        assertTrue(result.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void cambiarEstadoUsuario_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.cambiarEstadoUsuario(999L, true));
        verify(userRepository, never()).save(any());
    }

    @Test
    void actualizarUsuario_Success_UpdatesFields() {
        User existing = new User();
        existing.setId(1L);
        existing.setFullName("Nombre Viejo");
        existing.setEmail("mismo@ubb.cl");

        User updates = new User();
        updates.setFullName("Nombre Nuevo");
        updates.setEmail("mismo@ubb.cl");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = userService.actualizarUsuario(1L, updates);

        assertEquals("Nombre Nuevo", result.getFullName());
        verify(userRepository).save(existing);
    }

    @Test
    void actualizarUsuario_EmailConflict_ThrowsException() {
        User existing = new User();
        existing.setId(1L);
        existing.setEmail("actual@ubb.cl");

        User updates = new User();
        updates.setEmail("ocupado@ubb.cl");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("ocupado@ubb.cl")).thenReturn(new User());

        assertThrows(EmailAlreadyInUseException.class, () -> userService.actualizarUsuario(1L, updates));
        verify(userRepository, never()).save(any());
    }

    @Test
    void actualizarMiPerfil_Success() {
        User existing = new User();
        existing.setEmail("estudiante@ubb.cl");
        existing.setFullName("Nombre Anterior");

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFullName("Nuevo Nombre Perfil");
        request.setPhone("+56912345678");
        request.setPassword("NuevaClave123!");

        when(userRepository.findByEmail("estudiante@ubb.cl")).thenReturn(existing);
        when(passwordEncoder.encode("NuevaClave123!")).thenReturn("hashedNuevaClave");
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.actualizarMiPerfil("estudiante@ubb.cl", request);

        assertEquals("Nuevo Nombre Perfil", updated.getFullName());
        assertEquals("+56912345678", updated.getPhone());
        assertEquals("hashedNuevaClave", updated.getPasswordHash());
        verify(userRepository).save(existing);
    }

    @Test
    void procesarArchivoMasivo_DelegatesToBatchService() {
        MultipartFile mockFile = mock(MultipartFile.class);

        userService.procesarArchivoMasivo(mockFile);

        verify(userBatchImportService).procesarArchivoMasivo(mockFile);
    }

    @Test
    void obtenerTodosUsuarios_ReturnsList() {
        List<User> list = List.of(new User(), new User());
        when(userRepository.findAll()).thenReturn(list);

        List<User> result = userService.obtenerTodosUsuarios();

        assertEquals(2, result.size());
    }

    @Test
    void listarUsuariosPorRol_ReturnsList() {
        List<User> list = List.of(new User());
        when(userRepository.findByRoleName("Estudiante")).thenReturn(list);

        List<User> result = userService.listarUsuariosPorRol("Estudiante");

        assertEquals(1, result.size());
    }
}
