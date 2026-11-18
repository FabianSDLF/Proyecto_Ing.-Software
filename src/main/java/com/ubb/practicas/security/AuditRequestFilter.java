package com.ubb.practicas.security;

import com.ubb.practicas.service.AuditService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRequestFilter extends OncePerRequestFilter {
    private final AuditService audit;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth == null ? null : auth.getName();
        boolean completed = false;
        try { chain.doFilter(request, response); completed = true; }
        finally {
            if (uri.startsWith("/api/") && Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) {
                int status = completed ? response.getStatus() : 500;
                String action = request.getMethod();
                if (uri.equals("/api/auth/login")) action = status < 400 ? "LOGIN" : "LOGIN_FALLIDO";
                try { audit.record(email, action, uri, status, request.getRemoteAddr()); }
                catch (RuntimeException ex) { log.error("No se pudo persistir el evento de auditoría para {}", uri); }
            }
        }
    }
}
