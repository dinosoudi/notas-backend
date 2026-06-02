package com.taskflow.shared.config;

import com.taskflow.shared.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * Decisiones importantes:
 * - STATELESS: sin sesiones HTTP — cada request se autentica con JWT
 * - CSRF desactivado: no necesario en APIs REST stateless
 * - CORS configurado para permitir el frontend React en localhost:3000
 * - JwtFilter se agrega ANTES del filtro de autenticación de Spring
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF — no necesario en APIs REST con JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Sin sesiones HTTP — STATELESS porque usamos JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de autorización por endpoint
                .authorizeHttpRequests(auth -> auth

                        // ── Endpoints públicos — no requieren JWT ──────────
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/resend-verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-reset-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                        // ── Actuator — solo health público ─────────────────
                        //.requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ── Todo lo demás requiere JWT válido ──────────────
                        .anyRequest().authenticated()
                )

                // Agregar JwtFilter antes del filtro de autenticación estándar de Spring
                // Así el JWT se valida antes de que Spring intente autenticar por otros medios
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt para hashear contraseñas.
     * Cost 12 — balance entre seguridad y rendimiento.
     * Se inyecta en AuthService con @Autowired o constructor injection.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuración CORS para permitir el frontend React.
     * En producción cambiar allowedOrigins por el dominio real.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // En desarrollo: frontend React en localhost:3000
        // En producción: cambiar por el dominio real (ej. https://taskflow.com)
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"  // Vite dev server
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true); // necesario para cookies httpOnly (refreshToken)
        config.setMaxAge(3600L); // cachear preflight 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
