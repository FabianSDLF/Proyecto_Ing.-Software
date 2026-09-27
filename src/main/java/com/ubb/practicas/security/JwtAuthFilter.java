package com.ubb.practicas.security;

import com.ubb.practicas.model.User;
import com.ubb.practicas.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                User user = userRepository.findByEmail(email);

                // TR-03: Validar que el usuario exista y continúe activo en la base de datos
                if (user != null && Boolean.TRUE.equals(user.getActive())) {
                    String role = jwtUtil.extractRole(token);
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        String normalizedRole = role.toUpperCase().replace(" ", "_");
                        if (!normalizedRole.equals(role)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
                        }
                    }

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email, null, authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
