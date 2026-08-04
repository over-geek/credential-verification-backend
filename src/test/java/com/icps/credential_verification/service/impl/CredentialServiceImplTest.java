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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialServiceImplTest {

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private CredentialServiceImpl credentialService;

    @Test
    void createCredentialPersistsAndReturnsFullResponseShape() {
        UUID id = UUID.randomUUID();
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
        );
        CredentialRequestDto request = new CredentialRequestDto(
                "Ada",
                "Lovelace",
                "Computer Science",
                "ICPS University",
                "2021 - 2024",
                "First Class",
                photoFile
        );

        when(credentialRepository.existsByQrToken(anyString())).thenReturn(false);
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> {
            Credential credential = invocation.getArgument(0);
            credential.setId(id);
            return credential;
        });

        CredentialResponseDto response = credentialService.createCredential(request);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.chipUid()).isNull();
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.credentialClass()).isEqualTo("First Class");
        assertThat(response.hasPhoto()).isTrue();
        verify(credentialRepository).save(any(Credential.class));
        verify(credentialRepository).existsByQrToken(anyString());
    }

    @Test
    void createCredentialRejectsEmptyPhoto() {
        MockMultipartFile emptyPhoto = new MockMultipartFile(
                "photo",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );
        CredentialRequestDto request = new CredentialRequestDto(
                "Ada",
                "Lovelace",
                "Computer Science",
                "ICPS University",
                "2021 - 2024",
                "First Class",
                emptyPhoto
        );

        assertThatThrownBy(() -> credentialService.createCredential(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCredentialPhotoReturnsPhotoBytesAndContentType() {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);
        byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        credential.setPhoto(pngBytes);

        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));

        CredentialPhotoDto photoDto = credentialService.getCredentialPhoto(id);

        assertThat(photoDto.content()).isEqualTo(pngBytes);
        assertThat(photoDto.contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
    }

    @Test
    void getCredentialPhotoThrowsWhenNoPhotoLinked() {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);
        credential.setPhoto(null);

        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> credentialService.getCredentialPhoto(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void linkChipUidUpdatesExistingCredential() {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);

        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));
        when(credentialRepository.findByChipUid("04AABBCCDD")).thenReturn(Optional.empty());
        when(credentialRepository.save(credential)).thenReturn(credential);

        CredentialResponseDto response = credentialService.linkChipUid(id, new ChipUidRequestDto(" 04AABBCCDD "));

        assertThat(response.chipUid()).isEqualTo("04AABBCCDD");
        verify(credentialRepository).save(credential);
    }

    @Test
    void linkChipUidRejectsChipAlreadyLinkedToAnotherCredential() {
        UUID id = UUID.randomUUID();
        Credential credential = credential(id);
        Credential otherCredential = credential(UUID.randomUUID());
        otherCredential.setChipUid("04AABBCCDD");

        when(credentialRepository.findById(id)).thenReturn(Optional.of(credential));
        when(credentialRepository.findByChipUid("04AABBCCDD")).thenReturn(Optional.of(otherCredential));

        assertThatThrownBy(() -> credentialService.linkChipUid(id, new ChipUidRequestDto("04AABBCCDD")))
                .isInstanceOf(DuplicateChipUidException.class);
    }

    @Test
    void getCredentialByChipUidReturnsSameResponseShapeAsIdLookup() {
        Credential credential = credential(UUID.randomUUID());
        credential.setChipUid("04AABBCCDD");

        when(credentialRepository.findByChipUid("04AABBCCDD")).thenReturn(Optional.of(credential));

        CredentialResponseDto response = credentialService.getCredentialByChipUid("04AABBCCDD");

        assertThat(response.id()).isEqualTo(credential.getId());
        assertThat(response.chipUid()).isEqualTo("04AABBCCDD");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.credentialClass()).isEqualTo("First Class");
    }

    @Test
    void getCredentialByQrTokenReturnsSameResponseShapeAsIdLookup() {
        Credential credential = credential(UUID.randomUUID());
        credential.setQrToken("abc123opaque");

        when(credentialRepository.findByQrToken("abc123opaque")).thenReturn(Optional.of(credential));

        CredentialResponseDto response = credentialService.getCredentialByQrToken("abc123opaque");

        assertThat(response.id()).isEqualTo(credential.getId());
        assertThat(response.chipUid()).isNull();
        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.credentialClass()).isEqualTo("First Class");
    }

    @Test
    void getCredentialThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(credentialRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialService.getCredential(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCredentialByChipUidRejectsBlankChipUid() {
        assertThatThrownBy(() -> credentialService.getCredentialByChipUid(" "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCredentialByQrTokenRejectsBlankQrToken() {
        assertThatThrownBy(() -> credentialService.getCredentialByQrToken(" "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listCredentialsMapsAllRecords() {
        when(credentialRepository.findAll()).thenReturn(List.of(
                credential(UUID.randomUUID()),
                credential(UUID.randomUUID())
        ));

        List<CredentialResponseDto> credentials = credentialService.listCredentials();

        assertThat(credentials).hasSize(2);
    }

    private Credential credential(UUID id) {
        Credential credential = new Credential();
        credential.setId(id);
        credential.setFirstName("Ada");
        credential.setLastName("Lovelace");
        credential.setCourse("Computer Science");
        credential.setUniversity("ICPS University");
        credential.setDuration("2021 - 2024");
        credential.setCredentialClass("First Class");
        return credential;
    }
}
