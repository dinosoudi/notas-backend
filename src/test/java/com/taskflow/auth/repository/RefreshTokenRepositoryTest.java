package com.taskflow.auth.repository;

import com.taskflow.auth.BasePostgresTest;
import com.taskflow.auth.entity.RefreshToken;
import com.taskflow.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest extends BasePostgresTest {

    @Autowired
    private RefreshTokenRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private UUID userId;
    private String tokenHash = "abc123hash";
    private String otherTokenHash = "xyz789hash";
    private LocalDateTime futureDate;
    private LocalDateTime pastDate;

    @BeforeEach
    void setUp() {
        user = new User();
        // user.setId(UUID.randomUUID());  ← BORRA esta línea
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("some-hash");
        user.setAuthProvider(User.AuthProvider.EMAIL);
        entityManager.persist(user);
        entityManager.flush();
        userId = user.getId(); // Hibernate asigna el ID después del persist+flush

        futureDate = LocalDateTime.now().plusDays(1);
        pastDate = LocalDateTime.now().minusDays(1);
    }

    // Método helper para crear un RefreshToken sin persistir
    private RefreshToken buildToken(String hash, User owner, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash);
        token.setUser(owner);
        token.setExpiresAt(expiresAt);
        return token;
    }

    // Método helper para persistir un RefreshToken (usa TestEntityManager para flujos con @Modifying)
    private RefreshToken persistToken(String hash, User owner, LocalDateTime expiresAt) {
        RefreshToken token = buildToken(hash, owner, expiresAt);
        entityManager.persist(token);
        entityManager.flush();
        entityManager.clear();   // importante para que los @Modifying vean los cambios
        return token;
    }

    @Test
    void findByTokenHash_shouldReturnToken_whenExists() {
        persistToken(tokenHash, user, futureDate);

        Optional<RefreshToken> found = repository.findByTokenHash(tokenHash);

        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo(tokenHash);
        assertThat(found.get().getUser().getId()).isEqualTo(userId);
    }

    @Test
    void findByTokenHash_shouldReturnEmpty_whenNotFound() {
        Optional<RefreshToken> found = repository.findByTokenHash("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByTokenHash_shouldReturnTrue_whenHashExists() {
        persistToken(tokenHash, user, futureDate);

        boolean exists = repository.existsByTokenHash(tokenHash);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByTokenHash_shouldReturnFalse_whenHashDoesNotExist() {
        boolean exists = repository.existsByTokenHash("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void countByUserId_shouldReturnCorrectCount() {
        persistToken("hash1", user, futureDate);
        persistToken("hash2", user, futureDate);

        long count = repository.countByUserId(userId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUserId_shouldReturnZero_whenNoTokensForUser() {
        long count = repository.countByUserId(userId);

        assertThat(count).isZero();
    }

    @Test
    void deleteByTokenHash_shouldRemoveSpecificToken() {
        persistToken(tokenHash, user, futureDate);
        persistToken(otherTokenHash, user, futureDate);

        // Act: borramos solo el primer token
        repository.deleteByTokenHash(tokenHash);
        entityManager.flush();
        entityManager.clear();

        Optional<RefreshToken> deleted = repository.findByTokenHash(tokenHash);
        Optional<RefreshToken> remaining = repository.findByTokenHash(otherTokenHash);

        assertThat(deleted).isEmpty();
        assertThat(remaining).isPresent();
    }

    @Test
    void deleteAllByUserId_shouldRemoveAllTokensOfUser() {
        persistToken("hash1", user, futureDate);
        persistToken("hash2", user, futureDate);

        repository.deleteAllByUserId(userId);
        entityManager.flush();
        entityManager.clear();

        long count = repository.countByUserId(userId);
        assertThat(count).isZero();
    }

    @Test
    void deleteAllByUserIdExcept_shouldRemoveAllExceptSpecifiedToken() {
        persistToken(tokenHash, user, futureDate);
        persistToken(otherTokenHash, user, futureDate);
        persistToken("hash3", user, futureDate);

        repository.deleteAllByUserIdExcept(userId, otherTokenHash);
        entityManager.flush();
        entityManager.clear();

        // Solo debe quedar el token con otherTokenHash
        Optional<RefreshToken> preserved = repository.findByTokenHash(otherTokenHash);
        Optional<RefreshToken> removed1 = repository.findByTokenHash(tokenHash);
        Optional<RefreshToken> removed2 = repository.findByTokenHash("hash3");

        assertThat(preserved).isPresent();
        assertThat(removed1).isEmpty();
        assertThat(removed2).isEmpty();
    }

    @Test
    void deleteAllExpired_shouldRemoveOnlyExpiredTokens() {
        // Token expirado
        persistToken("expired", user, pastDate);
        // Token aún válido
        persistToken("valid", user, futureDate);

        repository.deleteAllExpired(LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        Optional<RefreshToken> shouldBeGone = repository.findByTokenHash("expired");
        Optional<RefreshToken> shouldStay = repository.findByTokenHash("valid");

        assertThat(shouldBeGone).isEmpty();
        assertThat(shouldStay).isPresent();
    }

    @Test
    void findValidToken_shouldReturnToken_whenNotExpired() {
        persistToken(tokenHash, user, futureDate);

        Optional<RefreshToken> result = repository.findValidToken(tokenHash, LocalDateTime.now());

        assertThat(result).isPresent();
        assertThat(result.get().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void findValidToken_shouldReturnEmpty_whenTokenExpired() {
        persistToken(tokenHash, user, pastDate);

        Optional<RefreshToken> result = repository.findValidToken(tokenHash, LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    void findValidToken_shouldReturnEmpty_whenHashNotFound() {
        Optional<RefreshToken> result = repository.findValidToken("nonexistent", LocalDateTime.now());

        assertThat(result).isEmpty();
    }
}