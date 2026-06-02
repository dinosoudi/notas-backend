package com.taskflow.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ses.SesClient;

import static org.assertj.core.api.Assertions.assertThat;

class AwsConfigTest {

    // ─── Dev con credenciales ─────────────────────────────────
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = AwsConfig.class)
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

    // ─── Dev sin credenciales ─────────────────────────────────
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = AwsConfig.class)
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
        void shouldCreateClientsWithDefaultProvider() {
            assertThat(sesClient).isNotNull();
            assertThat(s3Client).isNotNull();
        }
    }

    // ─── Prod ─────────────────────────────────────────────────
    @ExtendWith(SpringExtension.class)
    @ContextConfiguration(classes = AwsConfig.class)
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
        void shouldCreateAllProdClients() {
            assertThat(sesClient).isNotNull();
            assertThat(s3Client).isNotNull();
            assertThat(secretsManagerClient).isNotNull();
        }
    }
}