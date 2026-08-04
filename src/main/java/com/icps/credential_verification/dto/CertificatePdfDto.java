package com.icps.credential_verification.dto;

public record CertificatePdfDto(
        String filename,
        byte[] content
) {
}
