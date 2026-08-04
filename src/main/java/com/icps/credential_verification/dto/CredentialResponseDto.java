package com.icps.credential_verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record CredentialResponseDto(
        UUID id,
        @JsonProperty("chip_uid") String chipUid,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String course,
        String university,
        String duration,
        @JsonProperty("class") String credentialClass,
        @JsonProperty("has_photo") Boolean hasPhoto
) {
}
