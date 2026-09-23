package com.ubb.practicas.service.batch.parser;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.exception.FileProcessingException;
import com.ubb.practicas.service.batch.UserBatchParser;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
public class ExcelUserBatchParser implements UserBatchParser {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        if (contentType != null) {
            return contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || contentType.equals("application/vnd.ms-excel");
        }
        return false;
    }

    @Override
    public List<UserImportRecordDto> parse(InputStream inputStream) throws FileProcessingException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<UserImportRecordDto> records = new ArrayList<>();

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return Collections.emptyList();
            }

            // Omitir cabecera (fila 0)
            rowIterator.next();

            int rowNumber = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNumber++;

                Cell cellName = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellEmail = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellPass = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                Cell cellRole = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                if (cellName == null && cellEmail == null && cellRole == null) {
                    continue; // Fila vacía
                }

                String fullName = cellName != null ? formatter.formatCellValue(cellName).trim() : "";
                String email = cellEmail != null ? formatter.formatCellValue(cellEmail).trim() : "";
                String password = cellPass != null ? formatter.formatCellValue(cellPass).trim() : "";
                String roleName = cellRole != null ? formatter.formatCellValue(cellRole).trim() : "";

                if (fullName.isEmpty() && email.isEmpty()) {
                    continue;
                }

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
            throw new FileProcessingException("Error al procesar el archivo Excel: " + ex.getMessage(), ex);
        }
    }
}
