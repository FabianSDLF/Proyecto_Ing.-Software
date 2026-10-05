package com.ubb.practicas.dto;

import com.ubb.practicas.model.PortfolioDocument;
import com.ubb.practicas.model.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDocumentDTO {
    private Long id;
    private Long assignmentId;
    private DocumentType type;
    private String fileName;
    private String fileUrl;
    private String storedFileName;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadDate;

    public static PortfolioDocumentDTO fromEntity(PortfolioDocument doc) {
        if (doc == null) {
            return null;
        }
        return PortfolioDocumentDTO.builder()
                .id(doc.getId())
                .assignmentId(doc.getPracticeAssignment() != null ? doc.getPracticeAssignment().getId() : null)
                .type(doc.getType())
                .fileName(doc.getFileName())
                .fileUrl(doc.getFileUrl())
                .storedFileName(doc.getStoredFileName())
                .contentType(doc.getContentType())
                .sizeBytes(doc.getSizeBytes())
                .uploadDate(doc.getUploadDate())
                .build();
    }
}
