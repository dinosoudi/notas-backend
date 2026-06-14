package com.taskflow.shared.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * Servicio de envío de correos via AWS SES.
 *
 * En dev: usa credenciales del .env (AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY)
 * En prod: usa el rol IAM de la task en ECS — no necesita credenciales explícitas
 *
 * El email remitente debe estar verificado en SES.
 * En sandbox solo puedes enviar a emails verificados también.
 */
@Service
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final String fromEmail;
    private final String appUrl;

    public EmailService(
            SesClient sesClient,
            @Value("${aws.ses.from-email}") String fromEmail,
            @Value("${app.url:http://localhost:3000}") String appUrl) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
        this.appUrl = appUrl;
    }

    // ─── Verificación de email al registrarse ─────────────────
    public void sendVerificationEmail(String to, String name, String token) {
        String verifyUrl = appUrl + "/verify-email?token=" + token;
        String subject = "Verifica tu cuenta de TaskFlow";
        String body = String.format("""
                Hola %s,
                
                Gracias por registrarte en TaskFlow. Para activar tu cuenta haz click en el siguiente enlace:
                
                %s
                
                Este enlace expira en 24 horas.
                
                Si no creaste una cuenta, ignora este correo.
                
                — El equipo de TaskFlow
                """, name, verifyUrl);

        sendEmail(to, subject, body);
        log.info("📧 Correo de verificación enviado a: {}", to);
    }

    // ─── Código de 6 dígitos para recuperar contraseña ───────
    public void sendResetCodeEmail(String to, String name, String code) {
        String subject = "Código para restablecer tu contraseña";
        String body = String.format("""
                Hola %s,
                
                Tu código para restablecer la contraseña es:
                
                %s
                
                Este código expira en 10 minutos.
                
                Si no solicitaste este código, ignora este correo.
                
                — El equipo de TaskFlow
                """, name, code);

        sendEmail(to, subject, body);
        log.info("📧 Código de recuperación enviado a: {}", to);
    }

    // ─── Aviso cuando la cuenta es de Google ──────────────────
    public void sendGoogleAccountEmail(String to, String name) {
        String subject = "Tu cuenta usa Google para iniciar sesión";
        String body = String.format("""
                Hola %s,
                
                Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                
                Sin embargo, tu cuenta está vinculada con Google. Para acceder, usa el botón "Continuar con Google" en la página de inicio de sesión.
                
                Si no hiciste esta solicitud, ignora este correo.
                
                — El equipo de TaskFlow
                """, name);

        sendEmail(to, subject, body);
        log.info("📧 Aviso de cuenta Google enviado a: {}", to);
    }

    // ─── Confirmación de eliminación de cuenta ────────────────
    public void sendDeletionScheduledEmail(String to, String name, String scheduledDate) {
        String subject = "Tu cuenta será eliminada el " + scheduledDate;
        String body = String.format("""
                Hola %s,
                
                Tu cuenta de TaskFlow está programada para eliminarse el %s.
                
                Si cambias de opinión, puedes cancelar la eliminación iniciando sesión antes de esa fecha.
                
                — El equipo de TaskFlow
                """, name, scheduledDate);

        sendEmail(to, subject, body);
        log.info("📧 Confirmación de eliminación enviada a: {}", to);
    }

    // ─── Reenvío de verificación ──────────────────────────────
    public void resendVerificationEmail(String to, String name, String token) {
        String verifyUrl = appUrl + "/verify-email?token=" + token;
        String subject = "Nuevo enlace de verificación — TaskFlow";
        String body = String.format("""
                Hola %s,
                
                Aquí tienes un nuevo enlace para verificar tu cuenta:
                
                %s
                
                Este enlace expira en 24 horas.
                
                — El equipo de TaskFlow
                """, name, verifyUrl);

        sendEmail(to, subject, body);
        log.info("📧 Reenvío de verificación enviado a: {}", to);
    }

    // ─── Helper privado ───────────────────────────────────────
    private void sendEmail(String to, String subject, String body) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data(body)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(request);

        } catch (SesException e) {
            String errorCode = e.awsErrorDetails().errorCode();

            // En sandbox SES rechaza emails a destinatarios no verificados
            // En lugar de dar 500, caemos al stub para no interrumpir el flujo
            if ("MessageRejected".equals(errorCode) || "EmailAddressNotVerified".equals(errorCode)) {
                log.warn("⚠️  [SANDBOX] Email no verificado en SES: {}. Mostrando en consola:", to);
                log.warn("   → Asunto: {}", subject);
                log.warn("   → Cuerpo: {}", body);
                return; // no lanzar excepción — el flujo continúa normalmente
            }

            // Cualquier otro error sí es un problema real
            log.error("❌ Error enviando correo a {}: {}", to, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Error enviando correo: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}