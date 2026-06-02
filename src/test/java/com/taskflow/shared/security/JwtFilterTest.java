package com.taskflow.shared.security;

import com.taskflow.users.entity.User;
import com.taskflow.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - Filtro de autenticación JWT")
class JwtFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Casos sin autenticación")
    class NoAuthenticationCases {

        @Test
        @DisplayName("Sin header Authorization → continúa sin autenticar")
        void shouldSkipWhenNoAuthHeader() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Header Authorization no empieza con 'Bearer ' → sin autenticar")
        void shouldSkipWhenNotBearer() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Basic 12345");

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Token inválido según JwtService → sin autenticar")
        void shouldSkipWhenTokenInvalid() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
            when(jwtService.isValid(TOKEN)).thenReturn(false);

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Error al extraer userId → sin autenticar")
        void shouldSkipWhenExtractUserIdFails() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
            when(jwtService.isValid(TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(TOKEN)).thenThrow(new RuntimeException("Invalid token"));

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Usuario no encontrado o no activo")
    class UserNotFoundOrInactiveCases {

        @BeforeEach
        void setupValidToken() {
            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
            when(jwtService.isValid(TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
        }

        @Test
        @DisplayName("Usuario no existe en BD → no autentica")
        void shouldNotAuthenticateWhenUserNotFound() throws ServletException, IOException {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Usuario existe pero no está activo → no autentica")
        void shouldNotAuthenticateWhenUserNotActive() throws ServletException, IOException {
            User user = mock(User.class);
            when(user.isActive()).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            jwtFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Autenticación exitosa")
    class SuccessfulAuthenticationCases {

        private User activeUser;

        @BeforeEach
        void setupActiveUser() {
            activeUser = mock(User.class);
            when(activeUser.isActive()).thenReturn(true);

            when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
            when(jwtService.isValid(TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser));
        }

        @Test
        @DisplayName("Debe establecer autenticación en el SecurityContext")
        void shouldSetAuthenticationInContext() throws ServletException, IOException {
            jwtFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(org.springframework.security.core.userdetails.User.class);
            assertThat(((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername())
                    .isEqualTo(USER_ID.toString());
            assertThat(auth.getAuthorities()).isEmpty();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Debe incluir detalles de la petición en la autenticación")
        void shouldSetAuthenticationDetails() throws ServletException, IOException {
            jwtFilter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getDetails()).isNotNull();
        }
    }

    @Test
    @DisplayName("Si ya existe autenticación en el contexto, no la sobreescribe")
    void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {
        // Preparar token válido
        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtService.isValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(USER_ID);

        // Simular que ya hay una autenticación existente
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findById(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
        verify(filterChain).doFilter(request, response);
    }
}