package com.ubb.practicas.service.batch.parser;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.service.batch.UserBatchParser;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser de CSV robusto con soporte de comillas escapadas RFC 4180, detección automática
 * de delimitadores (coma / punto y coma) y manejo de UTF-8 BOM (BK-32).
 */
@Component
public class CsvUserBatchParser implements UserBatchParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return true;
        }
        if (contentType != null) {
            return contentType.equals("text/csv") || contentType.equals("application/csv")
                    || contentType.equals("text/plain");
        }
        return false;
    }

    @Override
    public List<UserImportRecordDto> parse(InputStream inputStream) throws FileProcessingException {
        List<UserImportRecordDto> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return records;
            }

            // Remover UTF-8 Byte Order Mark (BOM) si existe
            if (firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }

            // Detección automática de delimitador (; o ,)
            char delimiter = detectDelimiter(firstLine);

            String line;
            int rowNumber = 1; // La primera línea fue el encabezado

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> tokens = parseCsvLine(line, delimiter);
                if (tokens.size() < 4) {
                    continue;
                }

                String fullName = tokens.get(0).trim();
                String email = tokens.get(1).trim();
                String password = tokens.get(2).trim();
                String roleName = tokens.get(3).trim();

                records.add(UserImportRecordDto.builder()
                        .rowNumber(rowNumber)
                        .fullName(fullName)
                        .email(email)
                        .password(password)
                        .roleName(roleName)
                        .build());
            }

            return records;
        } catch (Exception ex) {
            throw new FileProcessingException("Error al leer el archivo CSV: " + ex.getMessage(), ex);
        }
    }

    private char detectDelimiter(String headerLine) {
        long semicolonCount = headerLine.chars().filter(ch -> ch == ';').count();
        long commaCount = headerLine.chars().filter(ch -> ch == ',').count();
        return semicolonCount > commaCount ? ';' : ',';
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // Saltar comilla escapada ("")
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}
