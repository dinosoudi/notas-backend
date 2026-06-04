package com.taskflow.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * Configuración de los clientes AWS SDK v2.
 *
 * En desarrollo (perfil dev): usa credenciales del .env / application-dev.yaml.
 * En producción (perfil prod): usa el rol IAM de la task en ECS Fargate — no necesita credenciales explícitas
 *
 * En prod NO necesitas access key ni secret key — el rol IAM del pod
 * tiene los permisos necesarios y AWS SDK los detecta solo.
 */
@Configuration
@Slf4j
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    // ─── SES ──────────────────────────────────────────────────

    @Bean
    @Profile("dev")
    public SesClient sesClientDev() {
        log.info("☁️  Iniciando SES Client en modo DEV");
        if (accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            log.warn("⚠️  AWS credentials no configuradas — SES no funcionará en dev. " +
                    "Configura AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY en .env");
            return SesClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();
    }

    @Bean
    @Profile("prod")
    public SesClient sesClientProd() {
        log.info("☁️  Iniciando SES Client en modo PROD — usando rol IAM de la task");
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // ─── Secrets Manager ──────────────────────────────────────

    @Bean
    @Profile("prod")
    public SecretsManagerClient secretsManagerClient() {
        log.info("☁️  Iniciando Secrets Manager Client en modo PROD");
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
