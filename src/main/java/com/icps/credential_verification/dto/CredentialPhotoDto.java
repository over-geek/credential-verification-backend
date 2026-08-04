package com.icps.credential_verification.dto;

public record CredentialPhotoDto(
        byte[] content,
        String contentType
) {
}
