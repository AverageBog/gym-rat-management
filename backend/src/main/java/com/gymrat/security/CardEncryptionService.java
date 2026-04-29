package com.gymrat.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CardEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    @Value("${app.card.encryption.key}")
    private String encryptionKeyBase64;

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            byte[] combined = new byte[IV_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_BYTES, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Card encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            byte[] ciphertext = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, buildKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Card decryption failed", e);
        }
    }

    // Returns "**** **** **** XXXX" — only the last 4 digits are exposed in API responses.
    public String mask(String encryptedCardNumber) {
        if (encryptedCardNumber == null || encryptedCardNumber.isBlank()) return null;
        String plaintext = decrypt(encryptedCardNumber);
        if (plaintext == null || plaintext.length() < 4) return "****";
        return "**** **** **** " + plaintext.substring(plaintext.length() - 4);
    }

    private SecretKeySpec buildKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
