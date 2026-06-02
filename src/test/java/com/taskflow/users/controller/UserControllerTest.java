package com.taskflow.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.shared.dto.MessageResponse;
import com.taskflow.shared.security.JwtService;
import com.taskflow.users.dto.*;
import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import com.taskflow.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    private static final String USER_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID USER_UUID = UUID.fromString(USER_UUID_STRING);

    private UserProfileResponse profileResponse;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        UserProfileResponse.PreferencesDTO prefs = new UserProfileResponse.PreferencesDTO(false, "es");
        profileResponse = UserProfileResponse.builder()
                .id(USER_UUID)
                .name("Juan Pérez")
                .email("juan@example.com")
                .phone("+521234567890")
                .emailVerified(true)
                .authProvider(User.AuthProvider.EMAIL)
                .preferences(prefs)
                .createdAt(LocalDateTime.now())
                .build();

        messageResponse = new MessageResponse("Operación exitosa");
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetProfileTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe retornar perfil del usuario autenticado")
        void shouldReturnProfile() throws Exception {
            when(userService.getProfile(USER_UUID)).thenReturn(profileResponse);

            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_UUID_STRING))
                    .andExpect(jsonPath("$.name").value("Juan Pérez"));
        }
    }

    @Nested
    @DisplayName("PATCH /users/me/name")
    class UpdateNameTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe actualizar el nombre correctamente")
        void shouldUpdateName() throws Exception {
            UpdateNameRequest request = new UpdateNameRequest("Ana García");
            when(userService.updateName(eq(USER_UUID), any())).thenReturn(profileResponse);

            mockMvc.perform(patch("/users/me/name")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Juan Pérez"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el nombre está vacío")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(patch("/users/me/name")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateNameRequest(""))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el nombre es demasiado corto")
        void shouldReturn400WhenNameTooShort() throws Exception {
            mockMvc.perform(patch("/users/me/name")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateNameRequest("A"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /users/me/phone")
    class UpdatePhoneTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe actualizar el teléfono")
        void shouldUpdatePhone() throws Exception {
            UpdatePhoneRequest request = new UpdatePhoneRequest("+521111111111");
            when(userService.updatePhone(eq(USER_UUID), any())).thenReturn(profileResponse);

            mockMvc.perform(patch("/users/me/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe permitir enviar null para eliminar teléfono")
        void shouldAllowNullPhone() throws Exception {
            UpdatePhoneRequest request = new UpdatePhoneRequest(null);
            when(userService.updatePhone(eq(USER_UUID), any())).thenReturn(profileResponse);

            mockMvc.perform(patch("/users/me/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si el formato es inválido")
        void shouldReturn400WhenPhoneInvalidFormat() throws Exception {
            mockMvc.perform(patch("/users/me/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePhoneRequest("12345"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /users/me/password")
    class ChangePasswordTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe cambiar contraseña correctamente")
        void shouldChangePassword() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass")
                    .newPassword("NewPass123")
                    .confirmPassword("NewPass123")
                    .build();

            when(userService.changePassword(eq(USER_UUID), eq(null), any()))
                    .thenReturn(messageResponse);

            mockMvc.perform(patch("/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Operación exitosa"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe enviar el refresh token en el header si está presente")
        void shouldPassRefreshTokenHeader() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass")
                    .newPassword("NewPass123")
                    .confirmPassword("NewPass123")
                    .closeOtherSessions(true)
                    .build();

            when(userService.changePassword(eq(USER_UUID), eq("refresh-token-value"), any()))
                    .thenReturn(messageResponse);

            mockMvc.perform(patch("/users/me/password")
                            .header("X-Refresh-Token", "refresh-token-value")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            ArgumentCaptor<ChangePasswordRequest> captor = ArgumentCaptor.forClass(ChangePasswordRequest.class);
            verify(userService).changePassword(eq(USER_UUID), eq("refresh-token-value"), captor.capture());
            ChangePasswordRequest captured = captor.getValue();
            assertThat(captured.getCurrentPassword()).isEqualTo("oldPass");
            assertThat(captured.getNewPassword()).isEqualTo("NewPass123");
            assertThat(captured.getCloseOtherSessions()).isTrue();
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si las contraseñas no cumplen validación")
        void shouldReturn400WhenValidationFails() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass")
                    .newPassword("short")
                    .confirmPassword("short")
                    .build();

            mockMvc.perform(patch("/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /users/me/preferences")
    class UpdatePreferencesTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe actualizar preferencias")
        void shouldUpdatePreferences() throws Exception {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(true, "en");
            UserProfileResponse.PreferencesDTO prefsDTO = new UserProfileResponse.PreferencesDTO(true, "en");
            when(userService.updatePreferences(eq(USER_UUID), any())).thenReturn(prefsDTO);

            mockMvc.perform(patch("/users/me/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.darkMode").value(true))
                    .andExpect(jsonPath("$.language").value("en"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si falta idioma")
        void shouldReturn400WhenLanguageMissing() throws Exception {
            UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                    .darkMode(true)
                    .language(null)
                    .build();

            mockMvc.perform(patch("/users/me/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si idioma no soportado")
        void shouldReturn400WhenLanguageInvalid() throws Exception {
            mockMvc.perform(patch("/users/me/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdatePreferencesRequest(true, "fr"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /users/me")
    class DeleteAccountTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe solicitar eliminación de cuenta")
        void shouldDeleteAccount() throws Exception {
            DeleteAccountRequest request = new DeleteAccountRequest("password", "ELIMINAR");
            when(userService.requestAccountDeletion(eq(USER_UUID), any()))
                    .thenReturn(messageResponse);

            mockMvc.perform(delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Operación exitosa"));
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si falta la contraseña")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            DeleteAccountRequest request = DeleteAccountRequest.builder()
                    .password(null)
                    .confirmation("ELIMINAR")
                    .build();

            mockMvc.perform(delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe devolver 400 si la confirmación no es ELIMINAR")
        void shouldReturn400WhenConfirmationInvalid() throws Exception {
            mockMvc.perform(delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteAccountRequest("password", "eliminar"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /users/me/cancel-deletion")
    class CancelDeletionTests {
        @Test
        @WithMockUser(username = USER_UUID_STRING)
        @DisplayName("Debe cancelar la eliminación")
        void shouldCancelDeletion() throws Exception {
            when(userService.cancelDeletion(USER_UUID)).thenReturn(messageResponse);

            mockMvc.perform(post("/users/me/cancel-deletion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Operación exitosa"));
        }
    }
}