package com.taskflow.shared.config;

import com.taskflow.shared.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SecurityConfigIntegrationTest.TestController.class})
@Import(SecurityConfig.class)           // Importamos la configuración de seguridad
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock del filtro JWT para que el contexto arranque sin sus dependencias reales
    @MockitoBean
    private JwtFilter jwtFilter;

    @Test
    void publicEndpointsShouldBeAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/verify-email")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/resend-verification")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/forgot-password")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/verify-reset-code")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/reset-password")).andExpect(status().isOk());
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isOk());
    }

    // Controlador dummy para que las peticiones tengan un endpoint real
    @RestController
    static class TestController {
        @PostMapping("/auth/register") @ResponseStatus(HttpStatus.OK) void register() {}
        @PostMapping("/auth/login") @ResponseStatus(HttpStatus.OK) void login() {}
        @PostMapping("/auth/verify-email") @ResponseStatus(HttpStatus.OK) void verifyEmail() {}
        @PostMapping("/auth/resend-verification") @ResponseStatus(HttpStatus.OK) void resendVerification() {}
        @PostMapping("/auth/forgot-password") @ResponseStatus(HttpStatus.OK) void forgotPassword() {}
        @PostMapping("/auth/verify-reset-code") @ResponseStatus(HttpStatus.OK) void verifyResetCode() {}
        @PostMapping("/auth/reset-password") @ResponseStatus(HttpStatus.OK) void resetPassword() {}
        @PostMapping("/auth/refresh") @ResponseStatus(HttpStatus.OK) void refresh() {}
        @GetMapping("/notes") @ResponseStatus(HttpStatus.OK) void notes() {}
    }
}