package com.gymrat.repository;

import com.gymrat.entity.Member;
import com.gymrat.entity.MemberStatus;
import com.gymrat.security.CardEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class MemberRepositoryTest {

    // base64 of "12345678901234567890123456789012" (32 bytes — valid AES-256 key)
    private static final String TEST_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @Autowired
    MemberRepository memberRepository;

    /**
     * Regression test for the card_number column-length bug.
     *
     * An AES-256-GCM ciphertext (12-byte IV + 16-byte ciphertext + 16-byte tag)
     * base64-encodes to ~60 characters. The column was previously declared with
     * length = 19 (the legacy plaintext width), which caused Hibernate to create
     * a VARCHAR(19) in H2 and silently truncate — or reject — the ciphertext.
     *
     * This test will fail if the @Column length is ever set below the minimum
     * ciphertext length, because the round-trip through the DB will either throw
     * or return a corrupted value that no longer decrypts.
     */
    @Test
    void cardNumber_encryptedCiphertext_survivesRoundTripThroughDatabase() {
        CardEncryptionService encryptionService = new CardEncryptionService();
        ReflectionTestUtils.setField(encryptionService, "encryptionKeyBase64", TEST_KEY);

        String encrypted = encryptionService.encrypt("4111111111111111");

        Member saved = memberRepository.save(Member.builder()
                .name("Test Member")
                .email("cardtest@example.com")
                .status(MemberStatus.ACTIVE)
                .joinDate(LocalDate.now())
                .cardNumber(encrypted)
                .build());

        memberRepository.flush();

        Member reloaded = memberRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCardNumber()).isEqualTo(encrypted);
        assertThat(encryptionService.decrypt(reloaded.getCardNumber())).isEqualTo("4111111111111111");
    }
}
