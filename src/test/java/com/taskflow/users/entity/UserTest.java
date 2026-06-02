package com.taskflow.users.entity;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Entidad User")
class UserTest {

    // ─── Lifecycle hooks ──────────────────────────────────────
    @Nested
    @DisplayName("Lifecycle hooks")
    class LifecycleTests {
        @Test
        @DisplayName("onCreate() debe establecer createdAt y updatedAt")
        void onCreateShouldSetBothTimestamps() {
            User user = new User();
            user.onCreate();

            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
        }

        @Test
        @DisplayName("onUpdate() debe refrescar solo updatedAt")
        void onUpdateShouldRefreshUpdatedAtOnly() {
            LocalDateTime before = LocalDateTime.now().minusDays(1);
            User user = User.builder()
                    .createdAt(before)
                    .updatedAt(before)
                    .build();
            user.onUpdate();

            assertThat(user.getUpdatedAt()).isAfter(before);
            assertThat(user.getCreatedAt()).isEqualTo(before);
        }
    }

    // ─── Helpers isDeleted() e isActive() ────────────────────
    @Nested
    @DisplayName("Método isDeleted()")
    class IsDeletedTests {
        @Test
        @DisplayName("Debe retornar false si deletedAt es null")
        void shouldReturnFalseWhenNotDeleted() {
            User user = User.builder().build();
            assertThat(user.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("Debe retornar true si deletedAt no es null")
        void shouldReturnTrueWhenSoftDeleted() {
            User user = User.builder()
                    .deletedAt(LocalDateTime.now())
                    .build();
            assertThat(user.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Método isActive()")
    class IsActiveTests {
        @Test
        @DisplayName("Debe retornar true si no está eliminada y email verificado")
        void shouldBeActiveWhenNotDeletedAndEmailVerified() {
            User user = User.builder()
                    .emailVerified(true)
                    .build(); // deletedAt = null
            assertThat(user.isActive()).isTrue();
        }

        @Test
        @DisplayName("Debe retornar false si está eliminada")
        void shouldNotBeActiveWhenDeleted() {
            User user = User.builder()
                    .deletedAt(LocalDateTime.now())
                    .emailVerified(true)
                    .build();
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false si email no verificado")
        void shouldNotBeActiveWhenEmailNotVerified() {
            User user = User.builder()
                    .emailVerified(false)
                    .build(); // deletedAt = null
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("Debe retornar false si emailVerified es null")
        void shouldNotBeActiveWhenEmailVerifiedIsNull() {
            User user = User.builder()
                    .emailVerified(null)
                    .build();
            assertThat(user.isActive()).isFalse(); // Boolean.TRUE.equals(null) == false
        }
    }

    // ─── Builder defaults ─────────────────────────────────────
    @Test
    @DisplayName("Builder.Default debe dejar authProvider en EMAIL")
    void builderShouldDefaultAuthProviderToEmail() {
        User user = User.builder().build();
        assertThat(user.getAuthProvider()).isEqualTo(User.AuthProvider.EMAIL);
    }

    @Test
    @DisplayName("Builder.Default debe dejar emailVerified en false")
    void builderShouldDefaultEmailVerifiedToFalse() {
        User user = User.builder().build();
        assertThat(user.getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Builder.Default debe dejar resetAttempts en 0")
    void builderShouldDefaultResetAttemptsToZero() {
        User user = User.builder().build();
        assertThat(user.getResetAttempts()).isZero();
    }

    @Test
    @DisplayName("Builder.Default debe inicializar la lista de tags vacía")
    void builderShouldInitializeTagsEmpty() {
        User user = User.builder().build();
        assertThat(user.getTags()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Builder.Default debe inicializar la lista de notas vacía")
    void builderShouldInitializeNotesEmpty() {
        User user = User.builder().build();
        assertThat(user.getNotes()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Builder.Default debe inicializar la lista de refreshTokens vacía")
    void builderShouldInitializeRefreshTokensEmpty() {
        User user = User.builder().build();
        assertThat(user.getRefreshTokens()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Builder.Default debe crear preferences con valores por defecto")
    void builderShouldDefaultPreferences() {
        User user = User.builder().build();
        assertThat(user.getPreferences()).isNotNull();
        assertThat(user.getPreferences().getDarkMode()).isFalse();
        assertThat(user.getPreferences().getLanguage()).isEqualTo("es");
    }

    // ─── Inner class UserPreferences ──────────────────────────
    @Nested
    @DisplayName("Clase interna UserPreferences")
    class UserPreferencesTests {
        @Test
        @DisplayName("Builder.Default debe dejar darkMode en false y language en 'es'")
        void builderDefaultsShouldBeApplied() {
            User.UserPreferences prefs = User.UserPreferences.builder().build();
            assertThat(prefs.getDarkMode()).isFalse();
            assertThat(prefs.getLanguage()).isEqualTo("es");
        }

        @Test
        @DisplayName("Debe permitir sobrescribir todos los campos")
        void allFieldsCanBeOverridden() {
            User.UserPreferences prefs = User.UserPreferences.builder()
                    .darkMode(true)
                    .language("en")
                    .build();
            assertThat(prefs.getDarkMode()).isTrue();
            assertThat(prefs.getLanguage()).isEqualTo("en");
        }
    }

    // ─── Enum AuthProvider ────────────────────────────────────
    @Test
    @DisplayName("AuthProvider debe tener los valores EMAIL y GOOGLE")
    void authProviderEnumShouldContainEmailAndGoogle() {
        assertThat(User.AuthProvider.values()).containsExactly(User.AuthProvider.EMAIL, User.AuthProvider.GOOGLE);
    }
}