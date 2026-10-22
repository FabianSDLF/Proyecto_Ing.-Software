package com.ubb.practicas.config;

import com.ubb.practicas.service.PracticeCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * HU5: Garantiza al arrancar que los tres ciclos de práctica estén registrados.
 * Es idempotente y se ejecuta en todos los perfiles (también en test con H2,
 * donde Flyway está desactivado y la migración V2 no corre).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PracticeCycleInitializer implements ApplicationRunner {

    private final PracticeCycleService practiceCycleService;

    @Override
    public void run(ApplicationArguments args) {
        practiceCycleService.registrarCiclosFaltantes();
    }
}
