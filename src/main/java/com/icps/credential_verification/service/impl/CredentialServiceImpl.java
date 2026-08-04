package com.icps.credential_verification.service.impl;

import com.icps.credential_verification.dto.ChipUidRequestDto;
import com.icps.credential_verification.dto.CredentialRequestDto;
import com.icps.credential_verification.dto.CredentialResponseDto;
import com.icps.credential_verification.exception.BadRequestException;
import com.icps.credential_verification.exception.DuplicateChipUidException;
import com.icps.credential_verification.exception.ResourceNotFoundException;
import com.icps.credential_verification.model.Credential;
import com.icps.credential_verification.repository.CredentialRepository;
import com.icps.credential_verification.service.CredentialService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CredentialServiceImpl implements CredentialService {

    private final CredentialRepository credentialRepository;

    public CredentialServiceImpl(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    public CredentialResponseDto createCredential(CredentialRequestDto request) {
        Credential credential = new Credential();
        credential.setQrToken(generateUniqueQrToken());
        credential.setFirstName(request.firstName());
        credential.setLastName(request.lastName());
        credential.setCourse(request.course());
        credential.setUniversity(request.university());
        credential.setDuration(request.duration());
        credential.setCredentialClass(request.credentialClass());

        return toResponse(credentialRepository.save(credential));
    }

    @Override
    public CredentialResponseDto linkChipUid(UUID id, ChipUidRequestDto request) {
        String chipUid = normalizeChipUid(request.chipUid());
        Credential credential = findCredential(id);

        credentialRepository.findByChipUid(chipUid)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateChipUidException("Chip UID is already linked to another credential.");
                });

        credential.setChipUid(chipUid);
        return toResponse(credentialRepository.save(credential));
    }

    @Override
    public CredentialResponseDto getCredential(UUID id) {
        return toResponse(findCredential(id));
    }

    @Override
    public CredentialResponseDto getCredentialByChipUid(String chipUid) {
        String normalizedChipUid = normalizeChipUid(chipUid);
        return credentialRepository.findByChipUid(normalizedChipUid)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found for chip UID."));
    }

    @Override
    public CredentialResponseDto getCredentialByQrToken(String qrToken) {
        String normalizedQrToken = normalizeQrToken(qrToken);
        return credentialRepository.findByQrToken(normalizedQrToken)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found for QR token."));
    }

    @Override
    public List<CredentialResponseDto> listCredentials() {
        return credentialRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Credential findCredential(UUID id) {
        return credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found."));
    }

    private String normalizeChipUid(String chipUid) {
        if (chipUid == null || chipUid.isBlank()) {
            throw new BadRequestException("chip_uid is required.");
        }
        return chipUid.trim();
    }

    private String normalizeQrToken(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new BadRequestException("qr_token is required.");
        }
        return qrToken.trim();
    }

    private String generateUniqueQrToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (credentialRepository.existsByQrToken(token));

        return token;
    }

    private CredentialResponseDto toResponse(Credential credential) {
        return new CredentialResponseDto(
                credential.getId(),
                credential.getChipUid(),
                credential.getFirstName(),
                credential.getLastName(),
                credential.getCourse(),
                credential.getUniversity(),
                credential.getDuration(),
                credential.getCredentialClass()
        );
    }
}
