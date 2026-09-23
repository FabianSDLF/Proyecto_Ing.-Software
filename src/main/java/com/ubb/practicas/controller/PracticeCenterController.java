package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.service.PracticeCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros")
@RequiredArgsConstructor
public class PracticeCenterController {

    private final PracticeCenterService practiceCenterService;

    @GetMapping
    public List<PracticeCenter> listarCentros() {
        return practiceCenterService.listarCentros();
    }
}
