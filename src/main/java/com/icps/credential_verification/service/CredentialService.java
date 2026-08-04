package com.icps.credential_verification.service;

import com.icps.credential_verification.dto.ChipUidRequestDto;
import com.icps.credential_verification.dto.CredentialRequestDto;
import com.icps.credential_verification.dto.CredentialResponseDto;

import java.util.List;
import java.util.UUID;

public interface CredentialService {

    CredentialResponseDto createCredential(CredentialRequestDto request);

    CredentialResponseDto linkChipUid(UUID id, ChipUidRequestDto request);

    CredentialResponseDto getCredential(UUID id);

    CredentialResponseDto getCredentialByChipUid(String chipUid);

    CredentialResponseDto getCredentialByQrToken(String qrToken);

    List<CredentialResponseDto> listCredentials();
}
