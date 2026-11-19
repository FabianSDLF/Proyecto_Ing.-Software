package com.ubb.practicas.controller;

import com.ubb.practicas.service.StatisticsService;
import com.ubb.practicas.model.enums.PracticeCycle;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;
    @GetMapping("/estadisticas")
    public Map<String, Object> statistics(Principal principal, @RequestParam(required=false) Long periodId,
            @RequestParam(defaultValue="false") boolean history, @RequestParam(required=false) PracticeCycle cycle,
            @RequestParam(required=false) Long subjectId) {
        return service.report(principal.getName(), periodId, history, cycle, subjectId);
    }
}
