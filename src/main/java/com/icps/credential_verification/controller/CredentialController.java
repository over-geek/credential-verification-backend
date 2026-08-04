package com.icps.credential_verification.controller;

import com.icps.credential_verification.dto.ChipUidRequestDto;
import com.icps.credential_verification.dto.CertificatePdfDto;
import com.icps.credential_verification.dto.CredentialRequestDto;
import com.icps.credential_verification.dto.CredentialResponseDto;
import com.icps.credential_verification.service.CertificatePdfService;
import com.icps.credential_verification.service.CredentialService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/credentials")
public class CredentialController {

    private final CredentialService credentialService;
    private final CertificatePdfService certificatePdfService;

    public CredentialController(CredentialService credentialService, CertificatePdfService certificatePdfService) {
        this.credentialService = credentialService;
        this.certificatePdfService = certificatePdfService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialResponseDto createCredential(@RequestBody CredentialRequestDto request) {
        return credentialService.createCredential(request);
    }

    @PatchMapping("/{id}/chip")
    public CredentialResponseDto linkChipUid(@PathVariable UUID id, @RequestBody ChipUidRequestDto request) {
        return credentialService.linkChipUid(id, request);
    }

    @GetMapping("/{id}")
    public CredentialResponseDto getCredential(@PathVariable UUID id) {
        return credentialService.getCredential(id);
    }

    @GetMapping("/by-chip/{chipUid}")
    public CredentialResponseDto getCredentialByChipUid(@PathVariable String chipUid) {
        return credentialService.getCredentialByChipUid(chipUid);
    }

    @GetMapping("/by-qr/{qrToken}")
    public CredentialResponseDto getCredentialByQrToken(@PathVariable String qrToken) {
        return credentialService.getCredentialByQrToken(qrToken);
    }

    @GetMapping
    public List<CredentialResponseDto> listCredentials() {
        return credentialService.listCredentials();
    }

    @GetMapping(value = "/{id}/certificate.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable UUID id) {
        CertificatePdfDto certificate = certificatePdfService.generateCertificate(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(certificate.filename())
                        .build()
                        .toString())
                .body(certificate.content());
    }
}
