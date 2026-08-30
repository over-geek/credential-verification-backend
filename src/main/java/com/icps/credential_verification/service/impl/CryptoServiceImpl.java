package com.icps.credential_verification.service.impl;

import com.icps.credential_verification.service.CryptoService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
public class CryptoServiceImpl implements CryptoService {

    // Hardcoded keys for POC to survive backend restarts
    private static final String AES_KEY_B64 = "O76aMoUOMOD5pa0l6WCcaDv6DTCCiaG4BKOMrc3Yqxo=";
    private static final String ED25519_PRIVATE_B64 = "MC4CAQAwBQYDK2VwBCIEIJ4n6wHEy9j35xKszqlaPQrfmhgYKZkRl5NhM329CIzV";

    private final SecretKey aesKey;
    private final PrivateKey signingKey;

    public CryptoServiceImpl() throws Exception {
        this.aesKey = new SecretKeySpec(Base64.getDecoder().decode(AES_KEY_B64), "AES");
        
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        this.signingKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(ED25519_PRIVATE_B64)));
    }

    @Override
    public byte[] encryptAndSign(String jsonPayload, String chipUid) throws Exception {
        // 1. Generate 12-byte IV for AES-GCM
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        // 2. Encrypt payload
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        
        byte[] payloadBytes = jsonPayload.getBytes("UTF-8");
        byte[] ciphertext = cipher.doFinal(payloadBytes);

        // 3. Create Signature over (chipUidBytes + iv + ciphertext)
        byte[] uidBytes = chipUid.getBytes("UTF-8");
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(signingKey);
        sig.update(uidBytes);
        sig.update(iv);
        sig.update(ciphertext);
        byte[] signature = sig.sign(); // Should be exactly 64 bytes for Ed25519

        // 4. Construct final payload: [Total Length (4)] + [IV (12)] + [Ciphertext + Tag (N+16)] + [Signature (64)]
        int totalPayloadLength = iv.length + ciphertext.length + signature.length;
        ByteBuffer buffer = ByteBuffer.allocate(4 + totalPayloadLength);
        buffer.putInt(totalPayloadLength);
        buffer.put(iv);
        buffer.put(ciphertext);
        buffer.put(signature);

        return buffer.array();
    }

    @Override
    public String signQrPayload(String jsonPayload) throws Exception {
        byte[] payloadBytes = jsonPayload.getBytes("UTF-8");
        
        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(signingKey);
        sig.update(payloadBytes);
        byte[] signature = sig.sign();
        
        return Base64.getEncoder().encodeToString(signature);
    }
}
