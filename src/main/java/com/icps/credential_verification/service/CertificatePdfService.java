package com.icps.credential_verification.service;

import com.icps.credential_verification.dto.CertificatePdfDto;

import java.util.UUID;

public interface CertificatePdfService {

    CertificatePdfDto generateCertificate(UUID credentialId);
}
