package com.taskflow.shared.security;

import com.taskflow.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro que se ejecuta UNA VEZ por cada request HTTP.
 * Extrae y valida el JWT del header Authorization.
 *
 * Flujo:
 * 1. Leer header Authorization: Bearer {token}
 * 2. Extraer y validar el token con JwtService
 * 3. Extraer userId del claim sub
 * 4. Verificar que el usuario existe y está activo en BD
 * 5. Setear la autenticación en el SecurityContext
 *
 * Si el token es inválido o falta, simplemente no setea la autenticación
 * y Spring Security rechaza el request con 401 automáticamente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token — quitar el prefijo "Bearer "
        final String token = authHeader.substring(7);

        // Validar el token
        if (!jwtService.isValid(token)) {
            log.debug("Token JWT inválido o expirado");
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer userId del claim sub
        UUID userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (Exception e) {
            log.debug("Error extrayendo userId del token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Si ya hay autenticación en el contexto, no sobreescribir
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Verificar que el usuario existe y está activo en BD
        // findActiveByEmail no — aquí buscamos por id directamente
        userRepository.findById(userId).ifPresent(user -> {
            if (user.isActive()) {
                // Construir el objeto de autenticación de Spring Security
                // Sin roles en v1 — lista vacía de authorities
                UserDetails userDetails = User.builder()
                        .username(userId.toString())
                        .password("")           // no necesario para JWT
                        .authorities(List.of()) // sin roles en v1
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Setear en el SecurityContext — a partir de aquí el request está autenticado
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        });

        filterChain.doFilter(request, response);
    }
}
