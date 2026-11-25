package com.ubb.practicas.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HelpController {
    private final String name, email, phone;
    public HelpController(@Value("${support.coordinator.name:}") String name,
            @Value("${support.coordinator.email:}") String email, @Value("${support.coordinator.phone:}") String phone) {
        this.name = name.trim(); this.email = email.trim(); this.phone = phone.trim();
    }
    @GetMapping("/api/ayuda/contacto")
    public Map<String, Object> contact() {
        boolean configured = !name.isBlank() && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
        return Map.of("configured", configured, "name", configured ? name : "", "email", configured ? email : "", "phone", configured ? phone : "");
    }
}
