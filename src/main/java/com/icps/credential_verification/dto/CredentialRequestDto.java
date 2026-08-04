package com.icps.credential_verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.multipart.MultipartFile;

public record CredentialRequestDto(
        @BindParam("first_name") @JsonProperty("first_name") String firstName,
        @BindParam("last_name") @JsonProperty("last_name") String lastName,
        String course,
        String university,
        String duration,
        @BindParam("class") @JsonProperty("class") String credentialClass,
        @BindParam("photo") MultipartFile photo
) {
}
