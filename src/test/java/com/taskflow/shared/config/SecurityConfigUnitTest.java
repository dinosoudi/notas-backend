package com.taskflow.shared.config;

import com.taskflow.shared.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigUnitTest {

    @Test
    void passwordEncoderShouldBeBCryptWithCost12() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtFilter.class));
        var encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isExactlyInstanceOf(BCryptPasswordEncoder.class);
        String hash = encoder.encode("test123");
        assertThat(hash).startsWith("$2a$12$");
    }

    @Test
    void corsConfigurationShouldDefineAllowedOriginsAndMethods() {
        SecurityConfig securityConfig = new SecurityConfig(mock(JwtFilter.class));
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        // Verificar que existe configuración para "/**"
        CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactlyInAnyOrder(
                "http://localhost:3000", "http://localhost:5173");
        assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactlyInAnyOrder(
                "Authorization", "Content-Type", "Accept");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }
}