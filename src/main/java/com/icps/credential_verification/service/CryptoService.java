package com.icps.credential_verification.service;

public interface CryptoService {
    /**
     * Encrypts and signs the payload, binding it cryptographically to the chip UID.
     * The resulting format is: [12 bytes IV] + [Ciphertext + 16 bytes GCM Tag] + [64 bytes Ed25519 Signature]
     *
     * @param jsonPayload The JSON credential data to encrypt
     * @param chipUid The physical NFC chip UID to bind this payload to
     * @return The raw binary offline payload ready to be written to the chip
     */
    byte[] encryptAndSign(String jsonPayload, String chipUid) throws Exception;
}
