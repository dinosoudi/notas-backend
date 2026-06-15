package com.taskflow.shared.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailService (con AWS SES mockeado)")
class EmailServiceTest {

    @Mock
    private SesClient sesClient;

    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@taskflow.com";
    private static final String APP_URL = "http://localhost:3000";
    private static final String TEST_TO = "test@example.com";
    private static final String TEST_NAME = "Juan";
    private static final String TEST_TOKEN = "abc-123";
    private static final String TEST_CODE = "123456";

    @BeforeEach
    void setUp() {
        emailService = new EmailService(sesClient, FROM_EMAIL, APP_URL);
    }

    // ─── Envío exitoso ─────────────────────────────────────────

    @Test
    @DisplayName("sendVerificationEmail debe invocar a SES sin errores")
    void sendVerificationEmail_success() {
        // given
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        // when / then
        assertThatCode(() -> emailService.sendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN))
                .doesNotThrowAnyException();

        // verify se llamó a sesClient exactamente una vez
        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("sendResetCodeEmail debe invocar a SES")
    void sendResetCodeEmail_success() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        assertThatCode(() -> emailService.sendResetCodeEmail(TEST_TO, TEST_NAME, TEST_CODE))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("sendGoogleAccountEmail debe invocar a SES")
    void sendGoogleAccountEmail_success() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        assertThatCode(() -> emailService.sendGoogleAccountEmail(TEST_TO, TEST_NAME))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("sendDeletionScheduledEmail debe invocar a SES")
    void sendDeletionScheduledEmail_success() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        assertThatCode(() -> emailService.sendDeletionScheduledEmail(TEST_TO, TEST_NAME, "2025-06-15"))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("resendVerificationEmail debe invocar a SES")
    void resendVerificationEmail_success() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().build());

        assertThatCode(() -> emailService.resendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    // ─── Verificación del contenido del correo ──────────────────

    @Test
    @DisplayName("sendVerificationEmail debe construir el enlace de verificación correctamente")
    void sendVerificationEmail_shouldContainCorrectUrl() throws Exception {
        // given
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        when(sesClient.sendEmail(captor.capture()))
                .thenReturn(SendEmailResponse.builder().build());

        // when
        emailService.sendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN);

        // then
        SendEmailRequest request = captor.getValue();
        String bodyText = request.message().body().text().data();

        assertThat(bodyText).contains(APP_URL + "/verify-email?token=" + TEST_TOKEN);
        assertThat(bodyText).contains(TEST_NAME);
        assertThat(request.source()).isEqualTo(FROM_EMAIL);
        assertThat(request.destination().toAddresses()).containsExactly(TEST_TO);
    }

    @Test
    @DisplayName("sendResetCodeEmail debe incluir el código de 6 dígitos")
    void sendResetCodeEmail_shouldContainCode() {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        when(sesClient.sendEmail(captor.capture()))
                .thenReturn(SendEmailResponse.builder().build());

        emailService.sendResetCodeEmail(TEST_TO, TEST_NAME, TEST_CODE);

        String bodyText = captor.getValue().message().body().text().data();
        assertThat(bodyText).contains(TEST_CODE);
    }

    // ─── Manejo del sandbox (destinatario no verificado) ──────

    @Test
    @DisplayName("Debe loguear warning y no lanzar excepción si SES rechaza por EmailAddressNotVerified")
    void sendEmail_whenSandboxRejection_shouldNotThrow() {
        // Creamos mocks de la excepción y sus detalles
        SesException exception = mock(SesException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn("EmailAddressNotVerified");
        when(exception.getMessage()).thenReturn("Email address not verified");

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(exception);

        assertThatCode(() -> emailService.sendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("Debe loguear warning y no lanzar excepción si SES rechaza por MessageRejected (sandbox)")
    void sendEmail_whenMessageRejected_shouldNotThrow() {
        SesException exception = mock(SesException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn("MessageRejected");
        when(exception.getMessage()).thenReturn("Message rejected");

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(exception);

        assertThatCode(() -> emailService.sendResetCodeEmail(TEST_TO, TEST_NAME, TEST_CODE))
                .doesNotThrowAnyException();

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("Debe lanzar RuntimeException si SES falla por otro motivo (ej: InvalidParameterValue)")
    void sendEmail_whenOtherSesError_shouldThrow() {
        SesException exception = mock(SesException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn("InvalidParameterValue");
        when(exception.getMessage()).thenReturn("Invalid parameter: source");

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(exception);

        assertThatCode(() -> emailService.sendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error enviando correo");
    }

    @Test
    @DisplayName("Debe lanzar RuntimeException si ocurre una excepción genérica de AWS")
    void sendEmail_whenGenericException_shouldThrow() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(new RuntimeException("AWS connection error"));

        assertThatCode(() -> emailService.sendVerificationEmail(TEST_TO, TEST_NAME, TEST_TOKEN))
                .isInstanceOf(RuntimeException.class);
    }
}