package com.icps.credential_verification.repository;

import com.icps.credential_verification.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByChipUid(String chipUid);

    Optional<Credential> findByQrToken(String qrToken);

    boolean existsByQrToken(String qrToken);
}
