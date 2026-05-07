package com.taskflow.shared.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de envío de correos.
 *
 * Por ahora es un stub — solo loguea los correos en consola.
 * La integración con AWS SES se implementa cuando lleguemos al deploy.
 *
 * Los métodos están definidos con la firma final para que AuthService
 * ya los llame correctamente y no haya que refactorizar después.
 */
@Service
@Slf4j
public class EmailService {

    // ─── Verificación de email al registrarse ─────────────────
    public void sendVerificationEmail(String to, String name, String token) {
        log.info("📧 [STUB] Enviando correo de verificación a: {}", to);
        log.info("   → Nombre: {}", name);
        log.info("   → Token: {}", token);
        log.info("   → URL: http://localhost:3000/verify-email?token={}", token);
    }

    // ─── Código de 6 dígitos para recuperar contraseña ───────
    public void sendResetCodeEmail(String to, String name, String code) {
        log.info("📧 [STUB] Enviando código de recuperación a: {}", to);
        log.info("   → Nombre: {}", name);
        log.info("   → Código: {}", code);
        log.info("   → Expira en 10 minutos");
    }

    // ─── Aviso cuando la cuenta es de Google ──────────────────
    public void sendGoogleAccountEmail(String to, String name) {
        log.info("📧 [STUB] Enviando aviso de cuenta Google a: {}", to);
        log.info("   → Nombre: {}", name);
        log.info("   → Mensaje: Esta cuenta usa Google para iniciar sesión");
    }

    // ─── Confirmación de eliminación de cuenta ────────────────
    public void sendDeletionScheduledEmail(String to, String name, String scheduledDate) {
        log.info("📧 [STUB] Enviando confirmación de eliminación a: {}", to);
        log.info("   → Nombre: {}", name);
        log.info("   → Fecha programada: {}", scheduledDate);
    }

    // ─── Reenvío de verificación ──────────────────────────────
    public void resendVerificationEmail(String to, String name, String token) {
        log.info("📧 [STUB] Reenviando correo de verificación a: {}", to);
        log.info("   → Token: {}", token);
    }
}