package com.gymrat.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CardEncryptionServiceTest {

    // base64 of "12345678901234567890123456789012" (32 bytes — valid AES-256 key)
    private static final String TEST_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @InjectMocks
    CardEncryptionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "encryptionKeyBase64", TEST_KEY);
    }

    @Test
    void encrypt_thenDecrypt_returnsOriginalPlaintext() {
        String plaintext = "4111111111111111";
        String encrypted = service.encrypt(plaintext);

        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(service.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    void encrypt_sameInput_producesDifferentCiphertexts() {
        // Each call generates a fresh random IV, so outputs must differ.
        String enc1 = service.encrypt("4111111111111111");
        String enc2 = service.encrypt("4111111111111111");
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void mask_exposesOnlyLastFourDigits() {
        String encrypted = service.encrypt("4111111111111111");
        assertThat(service.mask(encrypted)).isEqualTo("**** **** **** 1111");
    }

    @Test
    void mask_shortNumber_returnsFallback() {
        String encrypted = service.encrypt("123");
        assertThat(service.mask(encrypted)).isEqualTo("****");
    }

    @Test
    void encrypt_null_returnsNull() {
        assertThat(service.encrypt(null)).isNull();
    }

    @Test
    void encrypt_blank_returnsNull() {
        assertThat(service.encrypt("   ")).isNull();
    }

    @Test
    void decrypt_null_returnsNull() {
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void mask_null_returnsNull() {
        assertThat(service.mask(null)).isNull();
    }
}
