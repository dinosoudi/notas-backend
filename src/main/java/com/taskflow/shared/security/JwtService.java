package com.taskflow.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio para generar y validar JWT access tokens.
 *
 * Claims incluidos en el token:
 * - sub: userId (UUID del usuario)
 * - iat: fecha de emisión
 * - exp: fecha de expiración
 *
 * Sin roles en v1 — se agregan en v2 si se implementa admin.
 *
 * La clave secreta viene de application.yaml → JWT_SECRET en AWS.
 * Mínimo 64 caracteres para HS256.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final Long accessTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:86400000}") Long accessTokenExpirationMs) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    // ─── Generar access token ─────────────────────────────────

    public String generateAccessToken(UUID userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // ─── Validar y extraer claims ─────────────────────────────

    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String subject = getClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean isExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    // ─── Información de expiración ────────────────────────────

    public Long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    // Devuelve los segundos hasta expiración — para el TokensDTO
    public Long getAccessTokenExpiresIn() {
        return accessTokenExpirationMs / 1000;
    }

    // ─── Helper privado ───────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
