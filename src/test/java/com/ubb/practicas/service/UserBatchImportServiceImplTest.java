package com.ubb.practicas.service;

import com.ubb.practicas.dto.UserImportRecordDto;
import com.ubb.practicas.service.batch.parser.CsvUserBatchParser;
import com.ubb.practicas.service.batch.parser.ExcelUserBatchParser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserBatchImportServiceImplTest {

    private CsvUserBatchParser csvParser;
    private ExcelUserBatchParser excelParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvUserBatchParser();
        excelParser = new ExcelUserBatchParser();
    }

    @Test
    void testCsvParserSupports() {
        assertTrue(csvParser.supports("usuarios.csv", "text/csv"));
        assertTrue(csvParser.supports("data.CSV", null));
        assertFalse(csvParser.supports("archivo.xlsx", "application/vnd.ms-excel"));
    }

    @Test
    void testExcelParserSupports() {
        assertTrue(excelParser.supports("usuarios.xlsx", null));
        assertTrue(excelParser.supports("test.xls", null));
        assertFalse(excelParser.supports("test.csv", "text/csv"));
    }

    @Test
    void testCsvParserCommaAndQuotedField() {
        String csvContent = "Nombre,Email,Password,Rol\n" +
                "\"Sepúlveda, Fabián\",fabian@ubb.cl,pass123,Estudiante\n" +
                "Angelo Alumno,angelo@ubb.cl,pass456,Estudiante\n";

        List<UserImportRecordDto> records = csvParser.parse(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        assertEquals(2, records.size());
        assertEquals("Sepúlveda, Fabián", records.get(0).getFullName());
        assertEquals("fabian@ubb.cl", records.get(0).getEmail());
        assertEquals("pass123", records.get(0).getPassword());
        assertEquals("Estudiante", records.get(0).getRoleName());

        assertEquals("Angelo Alumno", records.get(1).getFullName());
        assertEquals("angelo@ubb.cl", records.get(1).getEmail());
    }

    @Test
    void testCsvParserSemicolonAndBom() {
        String csvContent = "\uFEFFNombre;Email;Password;Rol\n" +
                "Profesor Guia;guia@ubb.cl;;Profesor Colaborador\n";

        List<UserImportRecordDto> records = csvParser.parse(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, records.size());
        assertEquals("Profesor Guia", records.get(0).getFullName());
        assertEquals("guia@ubb.cl", records.get(0).getEmail());
        assertEquals("", records.get(0).getPassword());
        assertEquals("Profesor Colaborador", records.get(0).getRoleName());
    }

    @Test
    void testExcelParserNumericAndTextValues() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nombre");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Password");
            header.createCell(3).setCellValue("Rol");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Estudiante Uno");
            row.createCell(1).setCellValue("est1@ubb.cl");
            row.createCell(2).setCellValue(123456); // Numeric cell in Excel
            row.createCell(3).setCellValue("Estudiante");

            wb.write(bos);
        }

        List<UserImportRecordDto> records = excelParser.parse(new ByteArrayInputStream(bos.toByteArray()));
        assertEquals(1, records.size());
        assertEquals("Estudiante Uno", records.get(0).getFullName());
        assertEquals("est1@ubb.cl", records.get(0).getEmail());
        assertEquals("123456", records.get(0).getPassword());
        assertEquals("Estudiante", records.get(0).getRoleName());
    }
}
