package com.icps.credential_verification.service.impl;

import com.icps.credential_verification.dto.ChipUidRequestDto;
import com.icps.credential_verification.dto.CredentialPhotoDto;
import com.icps.credential_verification.dto.CredentialRequestDto;
import com.icps.credential_verification.dto.CredentialResponseDto;
import com.icps.credential_verification.exception.BadRequestException;
import com.icps.credential_verification.exception.DuplicateChipUidException;
import com.icps.credential_verification.exception.ResourceNotFoundException;
import com.icps.credential_verification.model.Credential;
import com.icps.credential_verification.repository.CredentialRepository;
import com.icps.credential_verification.service.CredentialService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
        if (request.photo() == null || request.photo().isEmpty()) {
            throw new BadRequestException("photo is required and cannot be empty.");
        }

        Credential credential = new Credential();
        credential.setQrToken(generateUniqueQrToken());
        credential.setFirstName(request.firstName());
        credential.setLastName(request.lastName());
        credential.setCourse(request.course());
        credential.setUniversity(request.university());
        credential.setDuration(request.duration());
        credential.setCredentialClass(request.credentialClass());

        try {
            byte[] photoBytes = request.photo().getBytes();
            if (photoBytes.length == 0) {
                throw new BadRequestException("photo is required and cannot be empty.");
            }
            credential.setPhoto(photoBytes);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read photo upload.");
        }

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
    public CredentialPhotoDto getCredentialPhoto(UUID id) {
        Credential credential = findCredential(id);
        byte[] photo = credential.getPhoto();
        if (photo == null || photo.length == 0) {
            throw new ResourceNotFoundException("Credential photo not found.");
        }
        return new CredentialPhotoDto(photo, detectContentType(photo));
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

    private String detectContentType(byte[] photo) {
        if (photo.length >= 4
                && (photo[0] == (byte) 0x89)
                && photo[1] == 'P'
                && photo[2] == 'N'
                && photo[3] == 'G') {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (photo.length >= 3
                && (photo[0] == (byte) 0xFF)
                && (photo[1] == (byte) 0xD8)
                && (photo[2] == (byte) 0xFF)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (photo.length >= 3
                && photo[0] == 'G'
                && photo[1] == 'I'
                && photo[2] == 'F') {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (photo.length >= 12
                && photo[0] == 'R' && photo[1] == 'I' && photo[2] == 'F' && photo[3] == 'F'
                && photo[8] == 'W' && photo[9] == 'E' && photo[10] == 'B' && photo[11] == 'P') {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
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
                credential.getCredentialClass(),
                credential.getPhoto() != null && credential.getPhoto().length > 0
        );
    }
}
