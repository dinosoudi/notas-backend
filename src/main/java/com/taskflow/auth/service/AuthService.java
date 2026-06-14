package com.taskflow.auth.service;

import com.taskflow.auth.dto.*;
import com.taskflow.auth.entity.RefreshToken;
import com.taskflow.auth.mapper.AuthMapper;
import com.taskflow.auth.repository.RefreshTokenRepository;
import com.taskflow.shared.dto.TokensDTO;
import com.taskflow.shared.dto.UserDTO;
import com.taskflow.shared.dto.MessageResponse;
import com.taskflow.shared.email.EmailService;
import com.taskflow.shared.exception.*;
import com.taskflow.shared.security.JwtService;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lógica de negocio del módulo de autenticación.
 *
 * Reglas generales:
 * - Nunca devolver si un email existe o no (seguridad contra enumeración)
 * - Hashear siempre contraseñas y códigos antes de guardar en BD
 * - @Transactional en métodos que hacen múltiples writes a BD
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    // Máximo intentos de login antes de bloquear
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int RESET_CODE_EXPIRY_MINUTES = 10;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;

    // ─── REGISTRO ─────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Validar que las contraseñas coincidan
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden", "confirmPassword");
        }

        // 2. Verificar que el email no esté registrado
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new ConflictException("Ya existe una cuenta con ese correo electrónico");
        }

        // 3. Crear el usuario
        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .authProvider(User.AuthProvider.EMAIL)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationExpires(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS))
                .preferences(new User.UserPreferences())
                .build();

        userRepository.save(user);

        // 4. Enviar correo de verificación
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);

        // 5. Devolver response SIN tokens — el usuario debe verificar su email primero
        UserDTO userDTO = authMapper.toUserDTO(user);
        return authMapper.toRegisterResponse(
                "Cuenta creada. Revisa tu correo para verificar tu cuenta.",
                userDTO
        );
    }

    // ─── VERIFICAR EMAIL ──────────────────────────────────────

    @Transactional
    public AuthResponse verifyEmail(String token) {

        // 1. Buscar usuario por token de verificación
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UnauthorizedException(
                        "El enlace de verificación no es válido."));

        // 2. Verificar que el token no haya expirado
        if (user.getVerificationExpires() == null ||
                LocalDateTime.now().isAfter(user.getVerificationExpires())) {
            throw new UnauthorizedException(
                    "El enlace de verificación expiró. Solicita uno nuevo.");
        }

        // 3. Verificar que no esté ya verificado
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ConflictException("Este correo ya fue verificado. Puedes iniciar sesión.");
        }

        // 4. Marcar como verificado y limpiar token
        userRepository.verifyEmail(user.getId());
        user.setEmailVerified(true);
        user.setVerificationToken(null);

        // 5. Generar tokens y devolver response con JWT — el usuario queda logueado
        TokensDTO tokens = generateTokens(user);
        UserDTO userDTO = authMapper.toUserDTO(user);

        return authMapper.toAuthResponse("Correo verificado. ¡Bienvenido!", userDTO, tokens);
    }

    // ─── LOGIN ────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {

        // 1. Buscar usuario por email — mismo mensaje si no existe o contraseña incorrecta
        User user = userRepository.findByEmail(request.getIdentifier().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));

        // 2. Verificar que no sea cuenta de Google
        if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            throw new UnauthorizedException(
                    "Esta cuenta usa Google para iniciar sesión. Usa el botón 'Continuar con Google'.");
        }

        // 3. Verificar límite de intentos fallidos
        if (user.getResetAttempts() >= MAX_LOGIN_ATTEMPTS) {
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Cuenta bloqueada por 15 minutos.", 900);
        }

        // 4. Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // Incrementar intentos fallidos
            user.setResetAttempts(user.getResetAttempts() + 1);
            userRepository.save(user);
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        }

        // 5. Verificar que el email esté verificado
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException(
                    "Debes verificar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada.");
        }

        // 6. Verificar que la cuenta no esté eliminada
        if (user.isDeleted()) {
            throw new UnauthorizedException(
                    "Esta cuenta está en proceso de eliminación.");
        }

        // 7. Resetear intentos fallidos al loguearse correctamente
        user.setResetAttempts(0);
        userRepository.save(user);

        // 8. Generar tokens y devolver response
        TokensDTO tokens = generateTokens(user);
        UserDTO userDTO = authMapper.toUserDTO(user);

        return authMapper.toAuthResponse("Inicio de sesión exitoso", userDTO, tokens);
    }

    // ─── OLVIDÉ MI CONTRASEÑA — PASO 1 ───────────────────────

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {

        // Respuesta genérica siempre — no revelar si el email existe
        String genericResponse = "Si ese correo está registrado, recibirás un código en los próximos minutos.";

        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {

            // Si es cuenta de Google, enviar correo especial indicando que use Google
            if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
                emailService.sendGoogleAccountEmail(user.getEmail(), user.getName());
                return;
            }

            // Verificar límite de reenvíos (reutilizamos resetAttempts para esto)
            if (user.getResetAttempts() >= 3) {
                return; // silenciosamente no hacer nada — el 429 lo maneja el rate limiter
            }

            // Generar código de 6 dígitos con SecureRandom
            String code = generateSixDigitCode();
            String codeHash = passwordEncoder.encode(code);

            user.setResetCode(codeHash);
            user.setResetCodeExpires(LocalDateTime.now().plusMinutes(RESET_CODE_EXPIRY_MINUTES));
            user.setResetAttempts(user.getResetAttempts() + 1);
            userRepository.save(user);

            emailService.sendResetCodeEmail(user.getEmail(), user.getName(), code);
        });

        return new MessageResponse(genericResponse);
    }

    // ─── OLVIDÉ MI CONTRASEÑA — PASO 2 ───────────────────────

    @Transactional
    public VerifyResetCodeResponse verifyResetCode(VerifyResetCodeRequest request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException(
                        "Código incorrecto. Verifica tu correo e intenta de nuevo."));

        // Verificar expiración del código
        if (user.getResetCode() == null || user.getResetCodeExpires() == null ||
                LocalDateTime.now().isAfter(user.getResetCodeExpires())) {
            throw new UnauthorizedException("El código ha expirado. Solicita uno nuevo.");
        }

        // Verificar intentos restantes
        int maxAttempts = 5;
        if (user.getResetAttempts() >= maxAttempts) {
            userRepository.clearResetCode(user.getId());
            throw new TooManyRequestsException(
                    "Demasiados intentos fallidos. Solicita un código nuevo.", null);
        }

        // Verificar el código
        if (!passwordEncoder.matches(request.getCode(), user.getResetCode())) {
            user.setResetAttempts(user.getResetAttempts() + 1);
            userRepository.save(user);
            int remaining = maxAttempts - user.getResetAttempts();
            throw new UnauthorizedException(
                    "Código incorrecto. Verifica tu correo e intenta de nuevo.", remaining);
        }

        // Código válido — generar resetToken temporal (15 min)
        String resetToken = UUID.randomUUID().toString();

        // campos dedicados para el reset token
        user.setResetToken(resetToken);
        user.setResetTokenExpires(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));

        user.setResetAttempts(0);
        userRepository.save(user);

        return authMapper.toVerifyResetCodeResponse(
                "Código verificado correctamente",
                resetToken,
                RESET_TOKEN_EXPIRY_MINUTES
        );
    }

    // ─── OLVIDÉ MI CONTRASEÑA — PASO 3 ───────────────────────

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {

        // 1. Validar que las contraseñas coincidan
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden", "confirmPassword");
        }

        // 2. Buscar usuario por resetToken
        User user = userRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new UnauthorizedException(
                        "El enlace de recuperación no es válido. Solicita uno nuevo."));

        // 3. Verificar expiración del resetToken

        if (user.getResetTokenExpires() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpires())) {
            throw new UnauthorizedException(
                    "El tiempo para cambiar tu contraseña expiró. Solicita un código nuevo.");
        }

        // 4. Verificar que la nueva contraseña no sea igual a la anterior
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException(
                    "La nueva contraseña no puede ser igual a la anterior", "newPassword");
        }

        // 5. Actualizar contraseña y limpiar tokens
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        user.setResetCode(null);
        user.setResetCodeExpires(null);
        user.setResetAttempts(0);
        user.setResetToken(null);
        user.setResetTokenExpires(null);
        userRepository.save(user);

        // 6. Invalidar todas las sesiones anteriores
        refreshTokenRepository.deleteAllByUserId(user.getId());

        // 7. Generar nuevos tokens — usuario queda logueado automáticamente
        TokensDTO tokens = generateTokens(user);
        UserDTO userDTO = authMapper.toUserDTO(user);

        return authMapper.toAuthResponse(
                "Contraseña actualizada correctamente", userDTO, tokens);

    }

    // ─── REFRESH TOKEN ────────────────────────────────────────

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {

        String incomingHash = hashToken(request.getRefreshToken());

        // 1. Buscar token válido (no expirado)
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(incomingHash, LocalDateTime.now())
                .orElseThrow(() -> new UnauthorizedException(
                        "Sesión expirada. Inicia sesión de nuevo."));

        User user = refreshToken.getUser();

        // 2. Invalidar el token usado (rotation — cada refresh genera uno nuevo)
        refreshTokenRepository.deleteByTokenHash(incomingHash);

        // 3. Generar nuevo access token y refresh token
        String newAccessToken = jwtService.generateAccessToken(user.getId());
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newRefreshHash = hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newRefreshHash)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshWebExpiresIn()))
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return authMapper.toRefreshResponse(
                newAccessToken,
                newRawRefreshToken,
                jwtService.getAccessTokenExpiresIn(),
                jwtService.getRefreshWebExpiresIn()
        );
    }

    // ─── LOGOUT ───────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }

    @Transactional
    public int logoutAll(UUID userId) {
        long count = refreshTokenRepository.countByUserId(userId);
        refreshTokenRepository.deleteAllByUserId(userId);
        return (int) count;
    }

    // ─── HELPERS PRIVADOS ─────────────────────────────────────

    private TokensDTO generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String rawRefreshToken = UUID.randomUUID().toString();
        String refreshHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshHash)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshWebExpiresIn()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return authMapper.toTokensDTO(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpiresIn(),
                jwtService.getRefreshWebExpiresIn()
        );
    }

    private String generateSixDigitCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) return;

            String token = UUID.randomUUID().toString();
            user.setVerificationToken(token);
            user.setVerificationExpires(
                    LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS));
            userRepository.save(user);

            emailService.resendVerificationEmail(user.getEmail(), user.getName(), token);
        });
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hasheando token", e);
        }
    }
}
