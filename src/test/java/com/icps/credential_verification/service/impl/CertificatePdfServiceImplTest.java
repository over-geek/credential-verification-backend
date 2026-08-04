package com.icps.credential_verification.service.impl;

import com.icps.credential_verification.dto.CertificatePdfDto;
import com.icps.credential_verification.exception.ResourceNotFoundException;
import com.icps.credential_verification.model.Credential;
import com.icps.credential_verification.repository.CredentialRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificatePdfServiceImplTest {

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private CertificatePdfServiceImpl certificatePdfService;

    @Test
    void generateCertificateReturnsParseablePdf() throws IOException {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);
        credential.setQrToken("abc123opaque");
        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));

        CertificatePdfDto certificate = certificatePdfService.generateCertificate(id);

        assertThat(certificate.filename()).isEqualTo("certificate-" + id + ".pdf");
        assertThat(new String(certificate.content(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        try (PDDocument document = Loader.loadPDF(certificate.content())) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);

            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Certificate of Completion");
            assertThat(text).contains("Ada Lovelace");
            assertThat(text).contains("Computer Science");
            assertThat(text).doesNotContain(id.toString());
            assertThat(text).doesNotContain("04AABBCCDD");
        }
    }

    @Test
    void generateCertificateBackfillsQrTokenForOlderCredential() {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);
        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));
        when(credentialRepository.existsByQrToken(anyString())).thenReturn(false);

        certificatePdfService.generateCertificate(id);

        assertThat(credential.getQrToken()).isNotBlank();
        verify(credentialRepository).save(credential);
    }

    @Test
    void generateCertificateThrowsWhenCredentialDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(credentialRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificatePdfService.generateCertificate(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Credential credential(UUID id) {
        Credential credential = new Credential();
        credential.setId(id);
        credential.setChipUid("04AABBCCDD");
        credential.setFirstName("Ada");
        credential.setLastName("Lovelace");
        credential.setCourse("Computer Science");
        credential.setUniversity("ICPS University");
        credential.setDuration("2021 - 2024");
        credential.setCredentialClass("First Class");
        return credential;
    }
}
