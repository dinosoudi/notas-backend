package com.taskflow.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de JWT como @ConfigurationProperties.
 *
 * Más limpio que múltiples @Value en JwtService — centraliza
 * todas las propiedades de JWT en una sola clase.
 *
 * Se lee desde application.yaml:
 * jwt:
 *   secret: ...
 *   access-expiration-ms: 86400000
 *   refresh-expiration-ms-web: 604800000
 *   refresh-expiration-ms-mobile: 2592000000
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    // Clave secreta para firmar tokens — mínimo 64 caracteres
    // En prod viene de AWS Secrets Manager como variable de entorno JWT_SECRET
    private String secret;

    // Access token — 24 horas (86400000 ms)
    private Long accessExpirationMs = 86400000L;

    // Refresh token web — 7 días (604800000 ms)
    private Long refreshExpirationMsWeb = 604800000L;

    // Refresh token móvil — 30 días (2592000000 ms) — v2
    private Long refreshExpirationMsMobile = 2592000000L;

    // ─── Helpers ──────────────────────────────────────────────

    // Segundos — para incluir en TokensDTO
    public Long getAccessExpiresInSeconds() {
        return accessExpirationMs / 1000;
    }

    public Long getRefreshWebExpiresInSeconds() {
        return refreshExpirationMsWeb / 1000;
    }

    public Long getRefreshMobileExpiresInSeconds() {
        return refreshExpirationMsMobile / 1000;
    }
}
