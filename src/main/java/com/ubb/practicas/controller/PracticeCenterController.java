package com.ubb.practicas.controller;

import com.ubb.practicas.model.PracticeCenter;
import com.ubb.practicas.repository.PracticeCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/centros")
public class PracticeCenterController {

    @Autowired
    private PracticeCenterRepository centerRepository;

    @GetMapping
    public List<PracticeCenter> listarCentros() {
        return centerRepository.findAll();
    }
}
