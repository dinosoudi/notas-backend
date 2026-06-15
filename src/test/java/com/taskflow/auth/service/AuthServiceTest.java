/*
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthMapper authMapper;
    @Mock private EmailService emailService;

    @InjectMocks private AuthService authService;

    // ================================================================
    // REGISTER
    // ================================================================
    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Debería registrar usuario y enviar correo de verificación")
        void shouldRegisterAndSendVerificationEmail() {
            RegisterRequest request = new RegisterRequest();
            request.setName("Test User");
            request.setEmail("test@example.com");
            request.setPassword("Password1!");
            request.setConfirmPassword("Password1!");
            request.setPhone("+123456789");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Password1!")).thenReturn("hashedPass");
            UserDTO mockUserDTO = new UserDTO();
            when(authMapper.toUserDTO(any(User.class))).thenReturn(mockUserDTO);
            AuthResponse expectedResponse = new AuthResponse();
            when(authMapper.toRegisterResponse(anyString(), eq(mockUserDTO)))
                    .thenReturn(expectedResponse);

            AuthResponse response = authService.register(request);

            // Verificar que se guardó el usuario
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("Test User");
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.getPasswordHash()).isEqualTo("hashedPass");
            assertThat(savedUser.getAuthProvider()).isEqualTo(User.AuthProvider.EMAIL);
            assertThat(savedUser.getEmailVerified()).isFalse();
            assertThat(savedUser.getVerificationToken()).isNotBlank();

            // Verificar envío de correo
            then(emailService).should().sendVerificationEmail(
                    "test@example.com", "Test User", savedUser.getVerificationToken());

            assertThat(response).isSameAs(expectedResponse);
        }

        @Test
        @DisplayName("Debe lanzar BadRequestException si las contraseñas no coinciden")
        void shouldThrowBadRequestWhenPasswordsMismatch() {
            RegisterRequest request = new RegisterRequest();
            request.setPassword("a");
            request.setConfirmPassword("b");

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Las contraseñas no coinciden");
            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Debe lanzar ConflictException si el email ya existe")
        void shouldThrowConflictWhenEmailExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("Password1!");
            request.setConfirmPassword("Password1!");
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Ya existe una cuenta");
        }
    }

    // ================================================================
    // VERIFY EMAIL
    // ================================================================
    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("Debe verificar email correctamente y devolver tokens")
        void shouldVerifyEmailAndReturnTokens() {
            User user = buildBasicUser();
            user.setEmailVerified(false);
            user.setVerificationToken("token123");
            user.setVerificationExpires(LocalDateTime.now().plusHours(1));

            when(userRepository.findByVerificationToken("token123")).thenReturn(Optional.of(user));
            TokensDTO tokens = new TokensDTO();
            when(authMapper.toUserDTO(user)).thenReturn(new UserDTO());
            AuthResponse expected = new AuthResponse();
            when(authMapper.toAuthResponse(anyString(), any(), eq(tokens)))
                    .thenReturn(expected);
            // mock generateTokens
            mockGenerateTokens(user, tokens);

            AuthResponse response = authService.verifyEmail("token123");

            assertThat(user.getEmailVerified()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            then(userRepository).should().verifyEmail(user.getId());
            then(refreshTokenRepository).should().save(any(RefreshToken.class));
            assertThat(response).isSameAs(expected);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si el token no existe")
        void shouldThrowUnauthorizedIfTokenNotFound() {
            when(userRepository.findByVerificationToken("bad")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.verifyEmail("bad"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si el token expiró")
        void shouldThrowUnauthorizedIfTokenExpired() {
            User user = buildBasicUser();
            user.setVerificationExpires(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByVerificationToken("expired")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("expired"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expiró");
        }

        @Test
        @DisplayName("Debe lanzar Conflict si ya estaba verificado")
        void shouldThrowConflictIfAlreadyVerified() {
            User user = buildBasicUser();
            user.setEmailVerified(true);
            user.setVerificationToken("token");
            user.setVerificationExpires(LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationToken("token")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("token"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("ya fue verificado");
        }
    }

    // ================================================================
    // LOGIN
    // ================================================================
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Debe loguear exitosamente y resetear intentos")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("user@example.com");
            request.setPassword("pass");

            User user = buildVerifiedUser();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", user.getPasswordHash())).thenReturn(true);
            TokensDTO tokens = new TokensDTO();
            mockGenerateTokens(user, tokens);
            when(authMapper.toUserDTO(user)).thenReturn(new UserDTO());
            AuthResponse expected = new AuthResponse();
            when(authMapper.toAuthResponse(anyString(), any(), eq(tokens))).thenReturn(expected);

            AuthResponse response = authService.login(request);

            assertThat(user.getResetAttempts()).isZero();
            then(userRepository).should().save(user);
            assertThat(response).isSameAs(expected);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si email no existe")
        void shouldThrowUnauthorizedIfEmailNotFound() {
            when(userRepository.findByEmail("no@user.com")).thenReturn(Optional.empty());
            LoginRequest request = new LoginRequest();
            request.setIdentifier("no@user.com");
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si es cuenta Google")
        void shouldThrowUnauthorizedIfGoogleAccount() {
            User user = buildVerifiedUser();
            user.setAuthProvider(User.AuthProvider.GOOGLE);
            when(userRepository.findByEmail("google@user.com")).thenReturn(Optional.of(user));
            LoginRequest request = new LoginRequest();
            request.setIdentifier("google@user.com");
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Google");
        }

        @Test
        @DisplayName("Debe lanzar TooManyRequests si intentos excedidos")
        void shouldThrowTooManyRequestsIfBlocked() {
            User user = buildVerifiedUser();
            user.setResetAttempts(5); // MAX_LOGIN_ATTEMPTS
            when(userRepository.findByEmail("blocked@user.com")).thenReturn(Optional.of(user));
            LoginRequest request = new LoginRequest();
            request.setIdentifier("blocked@user.com");
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("Debe incrementar intentos y lanzar Unauthorized si contraseña incorrecta")
        void shouldIncrementAttemptsOnWrongPassword() {
            User user = buildVerifiedUser();
            user.setResetAttempts(0);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);
            LoginRequest request = new LoginRequest();
            request.setIdentifier("user@example.com");
            request.setPassword("wrong");

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);
            then(userRepository).should().save(user);
            assertThat(user.getResetAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si email no verificado")
        void shouldThrowUnauthorizedIfEmailNotVerified() {
            User user = buildBasicUser(); // emailVerified = false
            when(userRepository.findByEmail("unverified@user.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            LoginRequest request = new LoginRequest();
            request.setIdentifier("unverified@user.com");
            request.setPassword("pass");
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("verificar tu correo");
        }
    }

    // ================================================================
    // FORGOT PASSWORD
    // ================================================================
    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("Debe devolver mensaje genérico si el email no existe")
        void shouldReturnGenericMessageIfEmailNotFound() {
            when(userRepository.findByEmail("no@user.com")).thenReturn(Optional.empty());
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("no@user.com");

            MessageResponse response = authService.forgotPassword(request);

            assertThat(response.getMessage()).contains("Si ese correo está registrado");
            then(emailService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Debe enviar código si el usuario existe y es cuenta normal")
        void shouldSendCodeForNormalUser() {
            User user = buildVerifiedUser();
            user.setResetAttempts(0);
            user.setEmail("user@example.com");  // ← clave
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("user@example.com");

            MessageResponse response = authService.forgotPassword(request);

            assertThat(response.getMessage()).contains("Si ese correo está registrado");
            then(userRepository).should().save(user);
            then(emailService).should().sendResetCodeEmail(eq("user@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("No debe hacer nada si se supera el límite de reenvíos")
        void shouldNotSendIfRateLimited() {
            User user = buildVerifiedUser();
            user.setResetAttempts(3); // límite
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("user@example.com");

            authService.forgotPassword(request);
            then(userRepository).should(never()).save(any());
            then(emailService).shouldHaveNoInteractions();
        }
    }

    // ... (continuación con verifyResetCode, resetPassword, refresh, logout, etc.)
    // Debido a la longitud, mostraré esqueletos para los más importantes.
    // El patrón es el mismo: simular los repositorios y verificar lógica.
    // Pondré los casos más destacados en un bloque anidado similar.

    // ================================================================
    // REFRESH TOKEN
    // ================================================================
    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("Debe rotar el refresh token y devolver nuevos tokens")
        void shouldRotateTokenAndReturnNewTokens() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("raw-refresh");

            // Stub del encoder con lógica condicional
            when(passwordEncoder.encode(anyString())).thenAnswer(inv -> {
                String arg = inv.getArgument(0);
                return "raw-refresh".equals(arg) ? "hashed-refresh" : "new-hash";
            });

            RefreshToken oldToken = new RefreshToken();
            User user = buildVerifiedUser();
            oldToken.setUser(user);

            when(refreshTokenRepository.findValidToken(eq("hashed-refresh"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(oldToken));

            when(jwtService.generateAccessToken(user.getId())).thenReturn("new-access");
            when(jwtService.getRefreshWebExpiresIn()).thenReturn(604800L);
            when(jwtService.getAccessTokenExpiresIn()).thenReturn(900L);

            RefreshResponse expected = new RefreshResponse();
            when(authMapper.toRefreshResponse(eq("new-access"), anyString(), eq(900L), eq(604800L)))
                    .thenReturn(expected);

            RefreshResponse response = authService.refresh(request);

            then(refreshTokenRepository).should().deleteByTokenHash("hashed-refresh");
            then(refreshTokenRepository).should().save(any(RefreshToken.class));
            assertThat(response).isSameAs(expected);
        }
    }

    // ... logs, helpers, etc.

    // Metodo auxiliar para construir usuarios de prueba
    private User buildBasicUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test")
                .email("test@example.com")
                .passwordHash("hash")
                .authProvider(User.AuthProvider.EMAIL)
                .emailVerified(false)
                .build();
    }

    private User buildVerifiedUser() {
        User user = buildBasicUser();
        user.setEmailVerified(true);
        return user;
    }

    // Para simular generateTokens (privado) sin invocarlo directamente, configuramos los mocks necesarios.
    private void mockGenerateTokens(User user, TokensDTO tokensDTO) {
        when(jwtService.generateAccessToken(user.getId())).thenReturn("access");
        when(passwordEncoder.encode(anyString())).thenReturn("refreshHash");
        when(jwtService.getRefreshWebExpiresIn()).thenReturn(604800L);
        when(jwtService.getAccessTokenExpiresIn()).thenReturn(900L);
        when(authMapper.toTokensDTO(eq("access"), anyString(), eq(900L), eq(604800L)))
                .thenReturn(tokensDTO);
    }

    @Nested
    @DisplayName("verifyResetCode")
    class VerifyResetCode {

        @Test
        @DisplayName("Debe verificar código correctamente y devolver resetToken")
        void shouldVerifyCodeAndReturnResetToken() {
            User user = buildVerifiedUser();
            user.setResetCode("hashedCode");
            user.setResetCodeExpires(LocalDateTime.now().plusMinutes(10));
            user.setResetAttempts(1); // menos del máximo

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("123456", "hashedCode")).thenReturn(true);

            VerifyResetCodeRequest request = new VerifyResetCodeRequest();
            request.setEmail("test@example.com");
            request.setCode("123456");

            VerifyResetCodeResponse expected = new VerifyResetCodeResponse();
            when(authMapper.toVerifyResetCodeResponse(anyString(), anyString(), anyInt()))
                    .thenReturn(expected);

            VerifyResetCodeResponse response = authService.verifyResetCode(request);

            assertThat(response).isSameAs(expected);
            assertThat(user.getResetAttempts()).isZero();
            assertThat(user.getResetToken()).isNotNull();
            assertThat(user.getResetTokenExpires()).isNotNull();
            then(userRepository).should().save(user);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si el email no existe")
        void shouldThrowIfEmailNotFound() {
            when(userRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());
            VerifyResetCodeRequest request = new VerifyResetCodeRequest();
            request.setEmail("no@existe.com");
            assertThatThrownBy(() -> authService.verifyResetCode(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si el código ha expirado")
        void shouldThrowIfCodeExpired() {
            User user = buildVerifiedUser();
            user.setResetCode("hashed");
            user.setResetCodeExpires(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            VerifyResetCodeRequest request = new VerifyResetCodeRequest();
            request.setEmail("test@example.com");
            request.setCode("123456");

            assertThatThrownBy(() -> authService.verifyResetCode(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expirado");
        }

        @Test
        @DisplayName("Debe lanzar TooManyRequests si excede intentos máximos")
        void shouldThrowTooManyRequestsIfMaxAttemptsReached() {
            User user = buildVerifiedUser();
            user.setResetAttempts(5); // maxAttempts = 5 en el código
            user.setResetCode("hashed");
            user.setResetCodeExpires(LocalDateTime.now().plusMinutes(10));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            VerifyResetCodeRequest request = new VerifyResetCodeRequest();
            request.setEmail("test@example.com");
            request.setCode("123456");

            assertThatThrownBy(() -> authService.verifyResetCode(request))
                    .isInstanceOf(TooManyRequestsException.class);
            then(userRepository).should().clearResetCode(user.getId());
        }


    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("Debe lanzar BadRequest si las contraseñas no coinciden")
        void shouldThrowIfPasswordsMismatch() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setNewPassword("pass1");
            request.setConfirmPassword("pass2");
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si resetToken no existe")
        void shouldThrowIfResetTokenNotFound() {
            when(userRepository.findByResetToken("invalid")).thenReturn(Optional.empty());
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setResetToken("invalid");
            request.setNewPassword("pass");
            request.setConfirmPassword("pass");
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Debe lanzar Unauthorized si resetToken expiró")
        void shouldThrowIfResetTokenExpired() {
            User user = buildVerifiedUser();
            user.setResetToken("expired");
            user.setResetTokenExpires(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByResetToken("expired")).thenReturn(Optional.of(user));

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setResetToken("expired");
            request.setNewPassword("pass");
            request.setConfirmPassword("pass");
            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Debe lanzar BadRequest si nueva contraseña es igual a la anterior")
        void shouldThrowIfNewPasswordSameAsOld() {
            User user = buildVerifiedUser();
            user.setResetToken("token");
            user.setResetTokenExpires(LocalDateTime.now().plusMinutes(15));
            user.setPasswordHash("oldHash");
            when(userRepository.findByResetToken("token")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("samePass", "oldHash")).thenReturn(true);

            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setResetToken("token");
            request.setNewPassword("samePass");
            request.setConfirmPassword("samePass");

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(BadRequestException.class);
        }
    }


    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("Debe eliminar el refresh token de la base de datos")
        void shouldDeleteRefreshToken() {
            String rawToken = "refresh-token";
            when(passwordEncoder.encode(rawToken)).thenReturn("hashedToken");

            authService.logout(rawToken);

            then(refreshTokenRepository).should().deleteByTokenHash("hashedToken");
        }
    }

    @Nested
    @DisplayName("logoutAll")
    class LogoutAll {

        @Test
        @DisplayName("Debe eliminar todos los tokens del usuario y devolver cantidad")
        void shouldDeleteAllTokensAndReturnCount() {
            UUID userId = UUID.randomUUID();
            when(refreshTokenRepository.countByUserId(userId)).thenReturn(5L);

            int count = authService.logoutAll(userId);

            assertThat(count).isEqualTo(5);
            then(refreshTokenRepository).should().deleteAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("resendVerification")
    class ResendVerification {

        @Test
        @DisplayName("Debe reenviar token de verificación si el usuario existe y no está verificado")
        void shouldResendTokenIfUserExistsAndNotVerified() {
            User user = buildBasicUser(); // emailVerified = false
            user.setEmail("test@example.com");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            authService.resendVerification("test@example.com");

            assertThat(user.getVerificationToken()).isNotNull();
            assertThat(user.getVerificationExpires()).isNotNull();
            then(userRepository).should().save(user);
            then(emailService).should().resendVerificationEmail(eq("test@example.com"), eq(user.getName()), anyString());
        }

        @Test
        @DisplayName("No debe hacer nada si el usuario no existe (respuesta silenciosa)")
        void shouldDoNothingIfUserNotFound() {
            when(userRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());

            authService.resendVerification("no@existe.com");

            then(userRepository).shouldHaveNoMoreInteractions();
            then(emailService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("No debe hacer nada si el usuario ya está verificado")
        void shouldDoNothingIfAlreadyVerified() {
            User user = buildVerifiedUser();
            user.setEmail("verified@example.com");
            when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(user));

            authService.resendVerification("verified@example.com");

            then(userRepository).should(never()).save(any());
            then(emailService).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("Debe lanzar Unauthorized si la cuenta está eliminada")
    void shouldThrowIfAccountDeleted() {
        User user = buildVerifiedUser();
        user.setDeletedAt(LocalDateTime.now());
        when(userRepository.findByEmail("deleted@user.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setIdentifier("deleted@user.com");
        request.setPassword("pass");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("eliminación");
    }
}

*/
