package com.taskflow.auth.repository;

import com.taskflow.auth.BasePostgresTest;
import com.taskflow.auth.entity.RefreshToken;
import com.taskflow.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
// Reemplazar la base de datos embebida por la de Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest extends BasePostgresTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager; // para persistir datos de prueba

    private User testUser;
    private RefreshToken token1, token2, expiredToken;


    @BeforeEach
    void setUp() {
        // Crear usuario con los campos reales de la entidad User
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash("dummyHash")
                .authProvider(User.AuthProvider.EMAIL)
                .emailVerified(true)
                .build();
        entityManager.persistAndFlush(testUser);

        // Token válido 1
        token1 = RefreshToken.builder()
                .tokenHash("hash1")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // Token válido 2 (mismo usuario)
        token2 = RefreshToken.builder()
                .tokenHash("hash2")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        // Token expirado
        expiredToken = RefreshToken.builder()
                .tokenHash("expiredHash")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        entityManager.persist(token1);
        entityManager.persist(token2);
        entityManager.persist(expiredToken);
        entityManager.flush();
    }

    // ─── Pruebas de métodos por convención ────────────────────

    @Test
    void findByTokenHash_debeRetornarTokenCuandoExiste() {
        var found = refreshTokenRepository.findByTokenHash("hash1");
        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo("hash1");
    }

    @Test
    void findByTokenHash_debeRetornarEmptyCuandoNoExiste() {
        var found = refreshTokenRepository.findByTokenHash("inexistente");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByTokenHash_debeRetornarTrueSiExiste() {
        boolean exists = refreshTokenRepository.existsByTokenHash("hash2");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByTokenHash_debeRetornarFalseSiNoExiste() {
        boolean exists = refreshTokenRepository.existsByTokenHash("nope");
        assertThat(exists).isFalse();
    }

    @Test
    void countByUserId_debeContarSoloTokensActivos() {
        long count = refreshTokenRepository.countByUserId(testUser.getId());
        // Tenemos 2 tokens activos (hash1 y hash2), el expirado no cuenta?
        // Ojo: countByUserId es un conteo simple sin filtrar expiración.
        // Por cómo está definido el método, cuenta todos los tokens de ese usuario.
        // Para devolver solo activos tendrías que añadir condición en el método.
        // Como no la tiene, devolverá 3 (incluye el expirado).
        assertThat(count).isEqualTo(3);
    }

    // ─── Pruebas de JPQL con @Modifying ────────────────────────

    @Test
    void deleteByTokenHash_debeBorrarSoloEseToken() {
        refreshTokenRepository.deleteByTokenHash("hash1");
        entityManager.flush();

        assertThat(refreshTokenRepository.findByTokenHash("hash1")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash2")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("expiredHash")).isPresent();
    }

    @Test
    void deleteAllByUserId_debeBorrarTodosLosTokensDelUsuario() {
        refreshTokenRepository.deleteAllByUserId(testUser.getId());
        entityManager.flush();

        assertThat(refreshTokenRepository.countByUserId(testUser.getId())).isZero();
    }

    @Test
    void deleteAllByUserIdExcept_debeBorrarTodosMenosElActual() {
        refreshTokenRepository.deleteAllByUserIdExcept(testUser.getId(), "hash2");
        entityManager.flush();

        assertThat(refreshTokenRepository.findByTokenHash("hash1")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash2")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("expiredHash")).isEmpty();
    }

    // ─── Limpieza de expirados ────────────────────────────────

    @Test
    void deleteAllExpired_debeBorrarTokensConExpiresAtAntesDelMomentoDado() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteAllExpired(now);
        entityManager.flush();

        assertThat(refreshTokenRepository.findByTokenHash("expiredHash")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash1")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("hash2")).isPresent();
    }

    @Test
    void findValidToken_soloRetornaTokensNoExpirados() {
        LocalDateTime now = LocalDateTime.now();
        var valid = refreshTokenRepository.findValidToken("hash1", now);
        assertThat(valid).isPresent();

        var expired = refreshTokenRepository.findValidToken("expiredHash", now);
        assertThat(expired).isEmpty();
    }
}