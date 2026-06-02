package com.taskflow.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("my-secret-key-very-long-at-least-64-characters");
        jwtConfig.setAccessExpirationMs(86_400_000L);   // 24 h
        jwtConfig.setRefreshExpirationMsWeb(604_800_000L); // 7 d
        jwtConfig.setRefreshExpirationMsMobile(2_592_000_000L); // 30 d
    }

    @Test
    void getAccessExpiresInSecondsShouldReturnCorrectValue() {
        assertThat(jwtConfig.getAccessExpiresInSeconds()).isEqualTo(86_400);
    }

    @Test
    void getRefreshWebExpiresInSecondsShouldReturnCorrectValue() {
        assertThat(jwtConfig.getRefreshWebExpiresInSeconds()).isEqualTo(604_800);
    }

    @Test
    void getRefreshMobileExpiresInSecondsShouldReturnCorrectValue() {
        assertThat(jwtConfig.getRefreshMobileExpiresInSeconds()).isEqualTo(2_592_000);
    }

    @Test
    void defaultValuesShouldBeSet() {
        JwtConfig defaultConfig = new JwtConfig();
        assertThat(defaultConfig.getAccessExpirationMs()).isEqualTo(86_400_000L);
        assertThat(defaultConfig.getRefreshExpirationMsWeb()).isEqualTo(604_800_000L);
        assertThat(defaultConfig.getRefreshExpirationMsMobile()).isEqualTo(2_592_000_000L);
    }
}