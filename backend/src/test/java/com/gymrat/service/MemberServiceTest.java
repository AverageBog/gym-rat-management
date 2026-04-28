package com.gymrat.service;

import com.gymrat.dto.MemberProfileDto;
import com.gymrat.dto.PaymentUpdateDto;
import com.gymrat.entity.Member;
import com.gymrat.entity.MemberStatus;
import com.gymrat.repository.AppUserRepository;
import com.gymrat.repository.MemberRepository;
import com.gymrat.repository.MembershipPlanRepository;
import com.gymrat.security.CardEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    // base64 of "12345678901234567890123456789012" (32 bytes — valid AES-256 key)
    private static final String TEST_KEY = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @Mock MemberRepository memberRepository;
    @Mock MembershipPlanRepository planRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock PasswordEncoder passwordEncoder;

    @Spy
    CardEncryptionService cardEncryptionService = new CardEncryptionService();

    @InjectMocks
    MemberService memberService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cardEncryptionService, "encryptionKeyBase64", TEST_KEY);
    }

    // -------------------------------------------------------------------------
    // updatePayment — card number encryption
    // -------------------------------------------------------------------------

    @Test
    void updatePayment_encryptsCardNumberBeforeStoring() {
        Member member = memberWithId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        ArgumentCaptor<Member> savedCaptor = ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(savedCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        PaymentUpdateDto dto = new PaymentUpdateDto("Visa", "4111111111111111", "12/27",
                "123 Main St", null, "Austin", "TX", "78701");

        memberService.updatePayment(1L, dto);

        String stored = savedCaptor.getValue().getCardNumber();
        assertThat(stored).isNotNull().isNotEqualTo("4111111111111111");
        // Verify the stored value decrypts back to the original card number.
        assertThat(cardEncryptionService.decrypt(stored)).isEqualTo("4111111111111111");
    }

    @Test
    void updatePayment_blankCardNumber_keepsExistingEncryptedValue() {
        String existingEncrypted = cardEncryptionService.encrypt("4111111111111111");
        Member member = memberWithId(1L);
        member.setCardNumber(existingEncrypted);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        ArgumentCaptor<Member> savedCaptor = ArgumentCaptor.forClass(Member.class);
        when(memberRepository.save(savedCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        PaymentUpdateDto dto = new PaymentUpdateDto("Visa", "", "12/27",
                "123 Main St", null, "Austin", "TX", "78701");

        memberService.updatePayment(1L, dto);

        assertThat(savedCaptor.getValue().getCardNumber()).isEqualTo(existingEncrypted);
    }

    // -------------------------------------------------------------------------
    // getAdminDetail / getMemberProfile — card number masking
    // -------------------------------------------------------------------------

    @Test
    void getAdminDetail_returnsMaskedCardNumber() {
        Member member = memberWithId(1L);
        member.setCardNumber(cardEncryptionService.encrypt("4111111111111111"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        var dto = memberService.getAdminDetail(1L);

        assertThat(dto.cardNumber()).isEqualTo("**** **** **** 1111");
    }

    @Test
    void getAdminDetail_noCardNumber_returnsNullCardNumber() {
        Member member = memberWithId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        var dto = memberService.getAdminDetail(1L);

        assertThat(dto.cardNumber()).isNull();
    }

    @Test
    void getMemberProfile_returnsMaskedCardNumber() {
        Member member = memberWithId(1L);
        member.setCardNumber(cardEncryptionService.encrypt("5500005555555559"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        MemberProfileDto dto = memberService.getMemberProfile(1L);

        assertThat(dto.cardNumber()).isEqualTo("**** **** **** 5559");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Member memberWithId(Long id) {
        return Member.builder()
                .id(id)
                .name("Test Member")
                .email("test@example.com")
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
