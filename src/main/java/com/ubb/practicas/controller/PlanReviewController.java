package com.ubb.practicas.controller;

import com.ubb.practicas.model.LessonPlan;
import com.ubb.practicas.service.PlanReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import java.security.Principal;

@RestController
@RequestMapping("/api/portafolio/planificaciones")
@RequiredArgsConstructor
public class PlanReviewController {
    private final PlanReviewService reviews;

    @GetMapping("/{id}/revision-ia")
    public JsonNode latest(@PathVariable Long id, Principal principal) {
        return reviews.latest(id, principal.getName());
    }

    @PostMapping("/{id}/revision-ia")
    public JsonNode review(@PathVariable Long id, Principal principal, @RequestHeader("Authorization") String bearer) {
        return reviews.review(id, principal.getName(), bearer);
    }

    @PostMapping("/{id}/enviar-docente")
    public LessonPlan submit(@PathVariable Long id, Principal principal) {
        return reviews.submit(id, principal.getName());
    }
}
