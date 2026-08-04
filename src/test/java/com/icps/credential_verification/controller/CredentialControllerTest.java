package com.icps.credential_verification.controller;

import com.icps.credential_verification.dto.CredentialPhotoDto;
import com.icps.credential_verification.dto.CredentialResponseDto;
import com.icps.credential_verification.service.CertificatePdfService;
import com.icps.credential_verification.service.CredentialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CredentialController.class)
class CredentialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CredentialService credentialService;

    @MockitoBean
    private CertificatePdfService certificatePdfService;

    @Test
    void createCredentialAcceptsMultipartFormData() throws Exception {
        UUID id = UUID.randomUUID();
        CredentialResponseDto response = new CredentialResponseDto(
                id,
                null,
                "Ada",
                "Lovelace",
                "Computer Science",
                "ICPS University",
                "2021 - 2024",
                "First Class",
                true
        );

        when(credentialService.createCredential(any())).thenReturn(response);

        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01}
        );

        mockMvc.perform(multipart("/credentials")
                        .file(photoFile)
                        .param("first_name", "Ada")
                        .param("last_name", "Lovelace")
                        .param("course", "Computer Science")
                        .param("university", "ICPS University")
                        .param("duration", "2021 - 2024")
                        .param("class", "First Class"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.first_name").value("Ada"))
                .andExpect(jsonPath("$.has_photo").value(true));
    }

    @Test
    void getCredentialPhotoReturnsImageBytesAndContentType() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] photoBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        when(credentialService.getCredentialPhoto(id))
                .thenReturn(new CredentialPhotoDto(photoBytes, MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get("/credentials/{id}/photo", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(photoBytes));
    }
}
