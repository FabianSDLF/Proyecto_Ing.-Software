package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.PlanStatus;
import com.ubb.practicas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PlanReviewService {
    private final LessonPlanRepository plans;
    private final ChatbotLogRepository reviews;
    private final ObjectMapper mapper;
    @Value("${chatbot.url:http://localhost:8000}") private String chatbotUrl;

    static String fingerprint(LessonPlan plan) {
        String text = String.join("\n", Objects.toString(plan.getVersion(), ""), Objects.toString(plan.getSchoolGrade(), ""),
                Objects.toString(plan.getMathAxis(), ""), Objects.toString(plan.getBaseLearningObjective(), ""),
                Objects.toString(plan.getObjectives(), ""), Objects.toString(plan.getContentJson(), ""),
                plan.getDocument() == null ? "" : Objects.toString(plan.getDocument().getStoredFileName(), ""));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private LessonPlan owned(Long id, String email) {
        LessonPlan plan = plans.findById(id).orElseThrow(() -> new NoSuchElementException("Planificación no encontrada"));
        if (!plan.isOwnedBy(email)) throw new AccessDeniedException("Solo el estudiante propietario puede revisar o enviar esta versión");
        if (!AssignmentScope.current(plan.getPracticeAssignment())) throw new IllegalArgumentException("La práctica está archivada");
        return plan;
    }

    @Transactional
    public JsonNode review(Long id, String email, String bearer) {
        LessonPlan plan = owned(id, email);
        JsonNode content;
        try { content = mapper.readTree(plan.getContentJson()); }
        catch (Exception e) { throw new IllegalArgumentException("Completa el formulario estructurado antes de enviarlo a IA"); }
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("schoolGrade", plan.getSchoolGrade());
        payload.put("mathAxis", Objects.toString(plan.getMathAxis(), ""));
        payload.put("baseLearningObjective", plan.getBaseLearningObjective());
        payload.put("specificObjectives", plan.getObjectives());
        for (String key : List.of("objetivoActitudinal", "duracion", "recursos", "evaluacion", "inicio", "desarrollo", "cierre", "inclusion"))
            payload.put(key, content.path(key).asText(""));
        for (String key : List.of("schoolGrade", "baseLearningObjective", "specificObjectives", "recursos", "evaluacion", "inicio", "desarrollo", "cierre", "inclusion"))
            if (payload.get(key) == null || payload.get(key).isBlank()) throw new IllegalArgumentException("Falta completar: " + key);
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(60000);
        String body = RestClient.builder().baseUrl(chatbotUrl).requestFactory(factory).build().post().uri("/api/evaluar-planificacion")
                .header("Authorization", bearer).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload).retrieve().body(String.class);
        JsonNode result = mapper.readTree(body);
        if (!result.has("aiApproved") || !result.has("is_mock") || !result.has("feedback"))
            throw new IllegalStateException("El servicio IA devolvió un resultado incompleto");
        ChatbotLog log = new ChatbotLog();
        log.setLessonPlan(plan);
        log.setContentHash(fingerprint(plan));
        log.setResultJson(body);
        log.setIsMock(result.path("is_mock").asBoolean(true));
        log.setAiApproved(result.path("aiApproved").asBoolean(false) && !log.getIsMock());
        String feedback = result.path("feedback").asText("");
        log.setAiFeedback(feedback.substring(0, Math.min(1000, feedback.length())));
        log.setIterationNumber(plan.getVersion());
        log.setReviewDate(LocalDateTime.now());
        reviews.save(log);
        plan.setStatus(log.getIsMock() ? PlanStatus.BORRADOR : log.getAiApproved() ? PlanStatus.APROBADO_IA : PlanStatus.RECHAZADO_IA);
        plans.save(plan);
        return result;
    }

    @Transactional
    public LessonPlan submit(Long id, String email) {
        LessonPlan plan = owned(id, email);
        ChatbotLog log = reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(id)
                .orElseThrow(() -> new IllegalArgumentException("Esta versión aún no tiene revisión IA"));
        if (!Boolean.TRUE.equals(log.getAiApproved()) || !Boolean.FALSE.equals(log.getIsMock()) || !fingerprint(plan).equals(log.getContentHash()))
            throw new IllegalArgumentException("Revisa con IA el contenido actual antes de enviarlo al docente; el modo local no certifica una validación IA");
        plan.setStatus(PlanStatus.EN_REVISION_DOCENTE);
        plan.setUpdatedAt(LocalDateTime.now());
        return plans.save(plan);
    }

    @Transactional(readOnly = true)
    public JsonNode latest(Long id, String email) {
        LessonPlan plan = plans.findById(id).orElseThrow(() -> new NoSuchElementException("Planificación no encontrada"));
        if (!plan.isAccessibleBy(email)) throw new AccessDeniedException("No pertenece a esta práctica");
        ChatbotLog log = reviews.findFirstByLessonPlanIdOrderByReviewDateDesc(id)
                .orElseThrow(() -> new NoSuchElementException("Esta versión no tiene revisión IA"));
        if (log.getResultJson() == null || !fingerprint(plan).equals(log.getContentHash()))
            throw new NoSuchElementException("No hay revisión vigente para este contenido");
        return mapper.readTree(log.getResultJson());
    }
}
