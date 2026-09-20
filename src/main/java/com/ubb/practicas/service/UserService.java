package com.ubb.practicas.service;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void procesarArchivoMasivo(MultipartFile archivo) throws Exception {
        String nombreArchivo = archivo.getOriginalFilename();

        if (nombreArchivo != null && nombreArchivo.endsWith(".xlsx")) {
            procesarExcel(archivo);
        } else if (nombreArchivo != null && nombreArchivo.endsWith(".csv")) {
            procesarCSV(archivo);
        } else {
            throw new Exception("Formato de archivo no soportado. Solo .csv o .xlsx");
        }
    }

    private void procesarExcel(MultipartFile archivo) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; 

                Cell cellName = row.getCell(0);
                Cell cellEmail = row.getCell(1);
                Cell cellPass = row.getCell(2);
                Cell cellRole = row.getCell(3);

                if (cellName == null || cellEmail == null || cellRole == null) continue;

                String fullName = cellName.getStringCellValue().trim();
                String email = cellEmail.getStringCellValue().trim();
                String password = cellPass != null ? cellPass.getStringCellValue().trim() : "temporal123";
                String roleName = cellRole.getStringCellValue().trim();

                guardarUsuarioBulk(fullName, email, password, roleName);
            }
        }
    }

    private void procesarCSV(MultipartFile archivo) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue; 
                }

                String[] datos = linea.split(",");
                if (datos.length < 4) continue; 

                String fullName = datos[0].trim();
                String email = datos[1].trim();
                String password = datos[2].trim();
                String roleName = datos[3].trim();

                guardarUsuarioBulk(fullName, email, password, roleName);
            }
        }
    }

    private void guardarUsuarioBulk(String fullName, String email, String password, String roleName) throws Exception {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw new Exception("Rol no encontrado en archivo: " + roleName);
        }
        
        User user = userRepository.findByEmail(email);
        if (user == null) {
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(true);
        }
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        
        userRepository.save(user);
    }

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

    @Transactional
    public User cambiarEstadoUsuario(Long usuarioId, boolean estado) throws Exception {
        User user = userRepository.findById(usuarioId)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
        user.setActive(estado);
        return userRepository.save(user);
    }

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
        
        // Encriptar la contraseña si viene (en GestionRoles envían la contraseña en el campo passwordHash)
        if (nuevoUsuario.getPasswordHash() != null) {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode(nuevoUsuario.getPasswordHash()));
        } else {
            nuevoUsuario.setPasswordHash(passwordEncoder.encode("temporal123"));
        }
        
        nuevoUsuario.setCreatedAt(LocalDateTime.now());
        nuevoUsuario.setActive(true);
        
        return userRepository.save(nuevoUsuario);
    }
    @Transactional
    public User actualizarMiPerfil(String emailActual, com.ubb.practicas.dto.UserProfileUpdateRequest request) throws Exception {
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