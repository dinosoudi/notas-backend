package com.taskflow.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ses.SesClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AwsConfig.class)
class AwsConfigTest {

    @Autowired
    private static ApplicationContext context;

    // ============================================================
    // Perfil "dev" con credenciales
    // ============================================================
    @SpringBootTest(classes = AwsConfig.class)
    @ActiveProfiles("dev")
    @TestPropertySource(properties = {
            "aws.region=us-east-1",
            "aws.access-key-id=fakeKey",
            "aws.secret-access-key=fakeSecret"
    })
    static class DevWithCredentialsTest {

        @Autowired(required = false)
        private SesClient sesClient;

        @Autowired(required = false)
        private S3Client s3Client;

        @Autowired(required = false)
        private SecretsManagerClient secretsManagerClient;

        @Test
        void shouldCreateSesAndS3Clients() {
            assertThat(sesClient).isNotNull();
            assertThat(s3Client).isNotNull();
        }

        @Test
        void shouldNotCreateSecretsManagerClientInDev() {
            assertThat(secretsManagerClient).isNull();
        }
    }

    // ============================================================
    // Perfil "dev" sin credenciales (usa DefaultCredentialsProvider)
    // ============================================================
    @SpringBootTest(classes = AwsConfig.class)
    @ActiveProfiles("dev")
    @TestPropertySource(properties = {
            "aws.region=us-east-1",
            "aws.access-key-id=",
            "aws.secret-access-key="
    })
    static class DevWithoutCredentialsTest {

        @Autowired(required = false)
        private SesClient sesClient;

        @Autowired(required = false)
        private S3Client s3Client;

        @Test
        void shouldStillCreateClientsWithDefaultProvider() {
            assertThat(sesClient).isNotNull();
            assertThat(s3Client).isNotNull();
            // No falla porque DefaultCredentialsProvider puede fallar en CI,
            // pero el bean se crea igual (solo fallará al usarlo).
            // Para tests unitarios, basta con que el contexto cargue.
        }
    }

    // ============================================================
    // Perfil "prod"
    // ============================================================
    @SpringBootTest(classes = AwsConfig.class)
    @ActiveProfiles("prod")
    @TestPropertySource(properties = "aws.region=us-east-1")
    static class ProdTest {

        @Autowired(required = false)
        private SesClient sesClient;

        @Autowired(required = false)
        private S3Client s3Client;

        @Autowired(required = false)
        private SecretsManagerClient secretsManagerClient;

        @Test
        void shouldCreateAllClients() {
            assertThat(sesClient).isNotNull();
            assertThat(s3Client).isNotNull();
            assertThat(secretsManagerClient).isNotNull();
        }
    }
}