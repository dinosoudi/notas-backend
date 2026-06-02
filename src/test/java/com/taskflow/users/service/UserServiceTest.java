package com.taskflow.users.service;

import com.taskflow.auth.repository.RefreshTokenRepository;
import com.taskflow.shared.dto.MessageResponse;
import com.taskflow.shared.email.EmailService;
import com.taskflow.shared.exception.BadRequestException;
import com.taskflow.shared.exception.ResourceNotFoundException;
import com.taskflow.shared.exception.UnauthorizedException;
import com.taskflow.users.dto.*;
import com.taskflow.users.entity.User;
import com.taskflow.users.mapper.UserMapper;
import com.taskflow.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;          // no se usa pero se necesita para @InjectMocks
    @Mock private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .name("Juan Pérez")
                .email("juan@example.com")
                .passwordHash("hashed_password")
                .phone("+521234567890")
                .emailVerified(true)
                .authProvider(User.AuthProvider.EMAIL)
                .preferences(new User.UserPreferences(false, "es"))
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
        // user no está eliminado (deletedAt = null)
    }

    // ─── getProfile ───────────────────────────────────────────
    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("Debe retornar perfil de usuario activo")
        void shouldReturnProfileForActiveUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.getProfile(userId);

            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getName()).isEqualTo("Juan Pérez");
            assertThat(response.getEmail()).isEqualTo("juan@example.com");
            assertThat(response.getPhone()).isEqualTo("+521234567890");
            assertThat(response.getEmailVerified()).isTrue();
            assertThat(response.getPreferences().getDarkMode()).isFalse();
            assertThat(response.getPreferences().getLanguage()).isEqualTo("es");
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el usuario no existe")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuario no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si la cuenta está eliminada")
        void shouldThrowExceptionWhenAccountDeleted() {
            user.setDeletedAt(LocalDateTime.now()); // soft deleted
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.getProfile(userId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Esta cuenta está en proceso de eliminación.");
        }
    }

    // ─── updateName ───────────────────────────────────────────
    @Nested
    @DisplayName("updateName")
    class UpdateNameTests {

        @Test
        @DisplayName("Debe actualizar el nombre correctamente")
        void shouldUpdateName() {
            UpdateNameRequest request = new UpdateNameRequest("  Ana García  ");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.updateName(userId, request);

            assertThat(response.getName()).isEqualTo("Ana García"); // trim
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            UpdateNameRequest request = new UpdateNameRequest("Nuevo");
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateName(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── updatePhone ──────────────────────────────────────────
    @Nested
    @DisplayName("updatePhone")
    class UpdatePhoneTests {

        @Test
        @DisplayName("Debe actualizar el teléfono")
        void shouldUpdatePhone() {
            UpdatePhoneRequest request = new UpdatePhoneRequest("+521111111111");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.updatePhone(userId, request);

            assertThat(response.getPhone()).isEqualTo("+521111111111");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Debe permitir eliminar el teléfono (null)")
        void shouldRemovePhoneWhenNull() {
            UpdatePhoneRequest request = new UpdatePhoneRequest(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.updatePhone(userId, request);

            assertThat(response.getPhone()).isNull();
            assertThat(user.getPhone()).isNull();
        }
    }

    // ─── changePassword ───────────────────────────────────────
    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        private ChangePasswordRequest request;
        private final String currentRefreshToken = "some-refresh-token-raw";

        @BeforeEach
        void setUpRequest() {
            request = new ChangePasswordRequest();
            request.setCurrentPassword("oldPass123");
            request.setNewPassword("NewPass456");
            request.setConfirmPassword("NewPass456");
            request.setCloseOtherSessions(false);
        }

        @Test
        @DisplayName("Debe cambiar contraseña exitosamente sin cerrar otras sesiones")
        void shouldChangePasswordWithoutClosingSessions() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", "hashed_password")).thenReturn(true);
            when(passwordEncoder.matches("NewPass456", "hashed_password")).thenReturn(false);
            when(passwordEncoder.encode("NewPass456")).thenReturn("new_hash");

            MessageResponse response = userService.changePassword(userId, currentRefreshToken, request);

            assertThat(response.getMessage()).contains("actualizada correctamente");
            verify(userRepository).save(user);
            verify(refreshTokenRepository, never()).deleteAllByUserIdExcept(any(), any());
            verify(refreshTokenRepository, never()).countByUserId(any());
        }

        @Test
        @DisplayName("Debe cerrar otras sesiones cuando closeOtherSessions=true")
        void shouldCloseOtherSessionsWhenRequested() {
            request.setCloseOtherSessions(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", "hashed_password")).thenReturn(true);
            when(passwordEncoder.matches("NewPass456", "hashed_password")).thenReturn(false);
            when(passwordEncoder.encode("NewPass456")).thenReturn("new_hash");
            // Simular que hay 2 sesiones antes y 1 después (se borró 1)
            when(refreshTokenRepository.countByUserId(userId)).thenReturn(2L, 1L);
            when(passwordEncoder.encode(currentRefreshToken)).thenReturn("hashed_refresh");

            MessageResponse response = userService.changePassword(userId, currentRefreshToken, request);

            assertThat(response.getMessage()).contains("Se cerraron 1 sesiones");
            assertThat(response.getSessionsClosedCount()).isEqualTo(1);
            verify(refreshTokenRepository).deleteAllByUserIdExcept(userId, "hashed_refresh");
        }

        @Test
        @DisplayName("Debe lanzar BadRequestException si las contraseñas nuevas no coinciden")
        void shouldThrowWhenPasswordsDontMatch() {
            request.setConfirmPassword("Different456");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.changePassword(userId, currentRefreshToken, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Las contraseñas no coinciden");
        }

        @Test
        @DisplayName("Debe lanzar BadRequestException si la contraseña actual es incorrecta")
        void shouldThrowWhenCurrentPasswordWrong() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(userId, currentRefreshToken, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("La contraseña actual es incorrecta");
        }

        @Test
        @DisplayName("Debe lanzar BadRequestException si la nueva es igual a la actual")
        void shouldThrowWhenNewPasswordSameAsCurrent() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", "hashed_password")).thenReturn(true);
            when(passwordEncoder.matches("NewPass456", "hashed_password")).thenReturn(true); // igual

            assertThatThrownBy(() -> userService.changePassword(userId, currentRefreshToken, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no puede ser igual a la actual");
        }
    }

    // ─── updatePreferences ────────────────────────────────────
    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("Debe actualizar preferencias de usuario")
        void shouldUpdatePreferences() {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(true, "en");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse.PreferencesDTO prefs = userService.updatePreferences(userId, request);

            assertThat(prefs.getDarkMode()).isTrue();
            assertThat(prefs.getLanguage()).isEqualTo("en");
            verify(userRepository).save(user);
        }
    }

    // ─── requestAccountDeletion ───────────────────────────────
    @Nested
    @DisplayName("requestAccountDeletion")
    class RequestAccountDeletionTests {

        private DeleteAccountRequest request;

        @BeforeEach
        void setUpRequest() {
            request = new DeleteAccountRequest("myPassword", "ELIMINAR");
        }

        @Test
        @DisplayName("Debe programar eliminación de cuenta y enviar email")
        void shouldScheduleDeletionAndSendEmail() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("myPassword", "hashed_password")).thenReturn(true);

            MessageResponse response = userService.requestAccountDeletion(userId, request);

            // Verificar soft delete
            ArgumentCaptor<LocalDateTime> deletedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> scheduledCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(userRepository).softDelete(eq(userId), deletedCaptor.capture(), scheduledCaptor.capture());
            LocalDateTime scheduledAt = scheduledCaptor.getValue();
            assertThat(scheduledAt).isAfter(LocalDateTime.now().plusDays(29)); // ~30 días

            // Verificar cierre de sesiones
            verify(refreshTokenRepository).deleteAllByUserId(userId);

            // Verificar envío de email
            verify(emailService).sendDeletionScheduledEmail(
                    eq("juan@example.com"), eq("Juan Pérez"), anyString());

            assertThat(response.getMessage()).contains("eliminada el");
            assertThat(response.getDeletionScheduledAt()).isEqualTo(scheduledAt);
        }

        @Test
        @DisplayName("Debe lanzar UnauthorizedException si contraseña incorrecta")
        void shouldThrowWhenPasswordWrong() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("myPassword", "hashed_password")).thenReturn(false);

            assertThatThrownBy(() -> userService.requestAccountDeletion(userId, request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Contraseña incorrecta");
        }
    }

    // ─── cancelDeletion ───────────────────────────────────────
    @Nested
    @DisplayName("cancelDeletion")
    class CancelDeletionTests {

        @Test
        @DisplayName("Debe cancelar la eliminación si la cuenta estaba en proceso")
        void shouldCancelDeletionWhenAccountScheduled() {
            user.setDeletedAt(LocalDateTime.now().minusDays(5));
            user.setDeletionScheduledAt(LocalDateTime.now().plusDays(25));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            MessageResponse response = userService.cancelDeletion(userId);

            verify(userRepository).cancelDeletion(userId);
            assertThat(response.getMessage()).contains("reactivada");
            assertThat(response.getDeletionScheduledAt()).isNull();
        }

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.cancelDeletion(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Debe lanzar BadRequestException si la cuenta no está en proceso de eliminación")
        void shouldThrowWhenNotScheduledForDeletion() {
            // deletedAt = null
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.cancelDeletion(userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no tiene una eliminación programada");
        }
    }
}