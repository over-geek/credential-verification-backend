package com.icps.credential_verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredentialRequestDto(
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String course,
        String university,
        String duration,
        @JsonProperty("class") String credentialClass
) {
}
