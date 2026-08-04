package com.icps.credential_verification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChipUidRequestDto(
        @JsonProperty("chip_uid") String chipUid
) {
}
