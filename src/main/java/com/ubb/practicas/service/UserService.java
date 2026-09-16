package com.ubb.practicas.service;

import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    // Método principal que decide si es Excel o CSV
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
        // Magia de Apache POI para leer Excel
        Workbook workbook = new XSSFWorkbook(archivo.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); // Leer la primera hoja

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Saltar la fila 0 (Encabezados)

            String fullName = row.getCell(0).getStringCellValue();
            String email = row.getCell(1).getStringCellValue();
            String password = row.getCell(2).getStringCellValue();
            String roleName = row.getCell(3).getStringCellValue();

            Role role = roleRepository.findByName(roleName);
            User user = userRepository.findByEmail(email);
            if (user == null) {
                user = new User();
            }
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            userRepository.save(user);
        }
        workbook.close();
    }

    private void procesarCSV(MultipartFile archivo) throws Exception {
        // Lógica tradicional de Java para leer texto separado por comas
        BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()));
        String linea;
        boolean primeraLinea = true;

        while ((linea = br.readLine()) != null) {
            if (primeraLinea) {
                primeraLinea = false;
                continue; // Saltar encabezados
            }

            // Separar la fila por comas
            String[] datos = linea.split(",");
            String fullName = datos[0];
            String email = datos[1];
            String password = datos[2];
            String roleName = datos[3];

            Role role = roleRepository.findByName(roleName);
            User user = userRepository.findByEmail(email);
            if (user == null) {
                user = new User();
            }
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            userRepository.save(user);
        }
    }

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
}