package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.BatchImportResultDto;
import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.exception.UnsupportedFileFormatException;
import com.ubb.practicas.model.Role;
import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.RoleRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.UserBatchImportService;
import com.ubb.practicas.service.batch.UserBatchParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchImportServiceImpl implements UserBatchImportService {

    private final List<UserBatchParser> parsers;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-user-password:temporal123}")
    private String defaultPassword;

    @Override
    @Transactional
    public BatchImportResultDto importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("El archivo proporcionado está vacío.");
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        UserBatchParser parser = parsers.stream()
                .filter(p -> p.supports(filename, contentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileFormatException(
                        "Formato de archivo no soportado. Formatos válidos: .xlsx, .xls, .csv"
                ));

        List<UserImportRecordDto> records;
        try (InputStream inputStream = file.getInputStream()) {
            records = parser.parse(inputStream);
        } catch (IOException e) {
            throw new FileProcessingException("No se pudo leer el archivo cargado: " + e.getMessage(), e);
        }

        BatchImportResultDto result = BatchImportResultDto.builder()
                .totalRecords(records.size())
                .build();

        for (UserImportRecordDto record : records) {
            try {
                processSingleRecord(record, result);
            } catch (Exception e) {
                String errMsg = "Fila " + record.getRowNumber() + " (" + record.getEmail() + "): " + e.getMessage();
                result.getErrors().add(errMsg);
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public void procesarArchivoMasivo(MultipartFile file) {
        BatchImportResultDto result = importUsers(file);
        if (result.getSuccessCount() == 0 && !result.getErrors().isEmpty()) {
            throw new FileProcessingException("Error al procesar archivo: " + result.getErrors().get(0));
        }
    }

    private void processSingleRecord(UserImportRecordDto record, BatchImportResultDto result) {
        if (record.getEmail() == null || record.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        if (record.getRoleName() == null || record.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }

        Role role = roleRepository.findByName(record.getRoleName().trim());
        if (role == null) {
            throw new IllegalArgumentException("Rol no encontrado en el sistema: " + record.getRoleName());
        }

        String rawPassword = (record.getPassword() != null && !record.getPassword().trim().isEmpty())
                ? record.getPassword().trim()
                : defaultPassword;

        User user = userRepository.findByEmail(record.getEmail().trim());
        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setCreatedAt(LocalDateTime.now());
            user.setActive(true);
            isNew = true;
        }

        user.setFullName(record.getFullName() != null ? record.getFullName().trim() : "");
        user.setEmail(record.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        userRepository.save(user);

        if (isNew) {
            result.getCreatedUserEmails().add(user.getEmail());
        } else {
            result.getUpdatedUserEmails().add(user.getEmail());
        }
        result.setSuccessCount(result.getSuccessCount() + 1);
    }
}
