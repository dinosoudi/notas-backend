package com.taskflow.users.service;

import com.taskflow.shared.dto.MessageResponse;
import com.taskflow.shared.email.EmailService;
import com.taskflow.shared.exception.*;
import com.taskflow.auth.repository.RefreshTokenRepository;
import com.taskflow.users.dto.*;
import com.taskflow.users.entity.User;
import com.taskflow.users.mapper.UserMapper;
import com.taskflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    // ─── GET /users/me ────────────────────────────────────────

    public UserProfileResponse getProfile(UUID userId) {
        User user = findActiveUser(userId);
        return toProfileResponse(user);
    }

    // ─── PATCH /users/me/name ─────────────────────────────────

    @Transactional
    public UserProfileResponse updateName(UUID userId, UpdateNameRequest request) {
        User user = findActiveUser(userId);
        user.setName(request.getName().trim());
        userRepository.save(user);
        return toProfileResponse(user);
    }

    // ─── PATCH /users/me/phone ────────────────────────────────

    @Transactional
    public UserProfileResponse updatePhone(UUID userId, UpdatePhoneRequest request) {
        User user = findActiveUser(userId);
        // null elimina el teléfono del perfil
        user.setPhone(request.getPhone());
        userRepository.save(user);
        return toProfileResponse(user);
    }

    // ─── PATCH /users/me/password ─────────────────────────────

    @Transactional
    public MessageResponse changePassword(UUID userId, String currentRefreshToken,
                                          ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        // 1. Validar que las contraseñas nuevas coincidan
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden", "confirmPassword");
        }

        // 2. Verificar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual es incorrecta", "currentPassword");
        }

        // 3. Verificar que la nueva no sea igual a la actual
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException(
                    "La nueva contraseña no puede ser igual a la actual", "newPassword");
        }

        // 4. Actualizar contraseña
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Cerrar otras sesiones si el usuario lo solicitó
        int closedSessions = 0;
        if (Boolean.TRUE.equals(request.getCloseOtherSessions()) && currentRefreshToken != null) {
            String currentHash = passwordEncoder.encode(currentRefreshToken);
            long before = refreshTokenRepository.countByUserId(userId);
            refreshTokenRepository.deleteAllByUserIdExcept(userId, currentHash);
            long after = refreshTokenRepository.countByUserId(userId);
            closedSessions = (int) (before - after);
        }

        MessageResponse response = new MessageResponse(
                closedSessions > 0
                        ? String.format("Contraseña actualizada. Se cerraron %d sesiones en otros dispositivos.", closedSessions)
                        : "Contraseña actualizada correctamente"
        );
        response.setSessionsClosedCount(closedSessions);
        return response;
    }

    // ─── PATCH /users/me/preferences ─────────────────────────

    @Transactional
    public UserProfileResponse.PreferencesDTO updatePreferences(
            UUID userId, UpdatePreferencesRequest request) {

        User user = findActiveUser(userId);

        User.UserPreferences prefs = new User.UserPreferences(
                request.getDarkMode(),
                request.getLanguage()
        );
        user.setPreferences(prefs);
        userRepository.save(user);

        return new UserProfileResponse.PreferencesDTO(
                prefs.getDarkMode(),
                prefs.getLanguage()
        );
    }

    // ─── DELETE /users/me ─────────────────────────────────────

    @Transactional
    public MessageResponse requestAccountDeletion(UUID userId, DeleteAccountRequest request) {
        User user = findActiveUser(userId);

        // 1. Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Contraseña incorrecta");
        }

        // 2. Soft delete — 30 días de gracia
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = now.plusDays(30);
        userRepository.softDelete(user.getId(), now, scheduledAt);

        // 3. Cerrar todas las sesiones
        refreshTokenRepository.deleteAllByUserId(userId);

        // 4. Enviar correo de confirmación
        String formattedDate = scheduledAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        emailService.sendDeletionScheduledEmail(user.getEmail(), user.getName(), formattedDate);

        MessageResponse response = new MessageResponse(
                String.format("Tu cuenta será eliminada el %s. Puedes cancelar este proceso antes de esa fecha.",
                        formattedDate)
        );
        response.setDeletionScheduledAt(scheduledAt);
        return response;
    }

    // ─── POST /users/me/cancel-deletion ──────────────────────

    @Transactional
    public MessageResponse cancelDeletion(UUID userId) {
        // Buscar incluyendo cuentas eliminadas
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getDeletedAt() == null) {
            throw new BadRequestException("Esta cuenta no tiene una eliminación programada.");
        }

        userRepository.cancelDeletion(userId);

        MessageResponse response = new MessageResponse("Tu cuenta ha sido reactivada correctamente.");
        response.setDeletionScheduledAt(null);
        return response;
    }

    // ─── HELPERS PRIVADOS ─────────────────────────────────────

    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.isDeleted()) {
            throw new UnauthorizedException("Esta cuenta está en proceso de eliminación.");
        }

        return user;
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse.PreferencesDTO prefsDTO = null;
        if (user.getPreferences() != null) {
            prefsDTO = new UserProfileResponse.PreferencesDTO(
                    user.getPreferences().getDarkMode(),
                    user.getPreferences().getLanguage()
            );
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .emailVerified(user.getEmailVerified())
                .authProvider(user.getAuthProvider())
                .preferences(prefsDTO)
                .createdAt(user.getCreatedAt())
                .deletionScheduledAt(user.getDeletionScheduledAt())
                .build();
    }
}
