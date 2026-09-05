package com.ubb.practicas.controller;

import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<User> obtenerTodosUsuarios() {
        return userRepository.findAll();
    }

    @PostMapping
    public User crearUsuario(@RequestBody User nuevoUsuario) {
        return userRepository.save(nuevoUsuario);
    }
}
