package com.ubb.practicas.util;

import com.ubb.practicas.model.User;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FeedbackThreadHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FeedbackThreadHelper() {
    }

    public static String appendComment(
            String existingFeedback,
            LocalDateTime existingDate,
            String newCommentText,
            User author,
            String fallbackEmail) {

        if (newCommentText == null || newCommentText.trim().isEmpty()) {
            return existingFeedback;
        }

        List<Map<String, Object>> comments = parseExistingComments(existingFeedback, existingDate);

        String authorName = (author != null && author.getFullName() != null && !author.getFullName().isBlank())
                ? author.getFullName().trim()
                : (fallbackEmail != null ? fallbackEmail : "Docente");

        String authorEmail = (author != null && author.getEmail() != null)
                ? author.getEmail().trim()
                : (fallbackEmail != null ? fallbackEmail : "");

        String authorRole = (author != null && author.getRole() != null && author.getRole().getName() != null)
                ? author.getRole().getName().trim()
                : "Docente";

        Map<String, Object> newEntry = new LinkedHashMap<>();
        newEntry.put("id", UUID.randomUUID().toString());
        newEntry.put("authorName", authorName);
        newEntry.put("authorEmail", authorEmail);
        newEntry.put("authorRole", authorRole);
        newEntry.put("text", newCommentText.trim());
        newEntry.put("date", LocalDateTime.now().toString());

        comments.add(newEntry);

        try {
            return OBJECT_MAPPER.writeValueAsString(comments);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar los comentarios de retroalimentación", e);
        }
    }

    private static List<Map<String, Object>> parseExistingComments(String existingFeedback, LocalDateTime existingDate) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (existingFeedback == null || existingFeedback.trim().isEmpty()) {
            return list;
        }

        String trimmed = existingFeedback.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<Map<String, Object>> parsed = OBJECT_MAPPER.readValue(
                        trimmed,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                if (parsed != null) {
                    list.addAll(parsed);
                    return list;
                }
            } catch (Exception ignored) {
                // Si no es JSON válido, tratar como comentario de texto plano previo
            }
        }

        Map<String, Object> legacyEntry = new LinkedHashMap<>();
        legacyEntry.put("id", "legacy-1");
        legacyEntry.put("authorName", "Docente Supervisor");
        legacyEntry.put("authorEmail", "");
        legacyEntry.put("authorRole", "Docente");
        legacyEntry.put("text", trimmed);
        legacyEntry.put("date", (existingDate != null ? existingDate : LocalDateTime.now()).toString());
        list.add(legacyEntry);
        return list;
    }
}
