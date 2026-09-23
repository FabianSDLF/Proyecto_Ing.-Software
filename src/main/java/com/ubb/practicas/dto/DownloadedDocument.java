package com.ubb.practicas.dto;

import org.springframework.core.io.Resource;

public record DownloadedDocument(
        Resource resource,
        String fileName,
        String contentType
) {}
