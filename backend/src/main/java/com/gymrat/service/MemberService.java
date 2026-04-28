package com.gymrat.service;

import com.gymrat.dto.*;
import com.gymrat.entity.*;
import com.gymrat.repository.AppUserRepository;
import com.gymrat.repository.MemberRepository;
import com.gymrat.repository.MembershipPlanRepository;
import com.gymrat.security.CardEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipPlanRepository planRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CardEncryptionService cardEncryptionService;

    public List<MemberDto> getAll(String status) {
        if (status != null && !status.isBlank()) {
            return memberRepository.findByStatus(parseStatus(status)).stream().map(this::toDto).toList();
        }
        return memberRepository.findAll().stream().map(this::toDto).toList();
    }

    public AdminMemberDto getAdminDetail(Long id) {
        return toAdminDto(findOrThrow(id));
    }

    public MemberProfileDto getMemberProfile(Long id) {
        return toProfileDto(findOrThrow(id));
    }

    public AdminMemberDto create(MemberDto dto) {
        memberRepository.findByEmail(dto.email()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
        });

        MembershipPlan plan = resolvePlan(dto.membershipPlanId());
        Member member = memberRepository.save(Member.builder()
                .name(dto.name())
                .email(dto.email())
                .phone(dto.phone())
                .joinDate(dto.joinDate() != null ? dto.joinDate() : LocalDate.now())
                .status(dto.status() != null ? parseStatus(dto.status()) : MemberStatus.ACTIVE)
                .membershipPlan(plan)
                .build());

        appUserRepository.save(AppUser.builder()
                .email(member.getEmail())
                .password(passwordEncoder.encode("Member123!"))
                .role(UserRole.MEMBER)
                .member(member)
                .build());

        return toAdminDto(member);
    }

    public AdminMemberDto update(Long id, MemberDto dto) {
        Member member = findOrThrow(id);
        memberRepository.findByEmail(dto.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
            }
        });
        member.setName(dto.name());
        member.setEmail(dto.email());
        member.setPhone(dto.phone());
        member.setJoinDate(dto.joinDate());
        member.setStatus(dto.status() != null ? parseStatus(dto.status()) : member.getStatus());
        member.setMembershipPlan(resolvePlan(dto.membershipPlanId()));
        return toAdminDto(memberRepository.save(member));
    }

    public MemberDto updateStatus(Long id, String status) {
        Member member = findOrThrow(id);
        member.setStatus(parseStatus(status));
        return toDto(memberRepository.save(member));
    }

    public MemberProfileDto updateContact(Long id, ContactUpdateDto dto) {
        Member member = findOrThrow(id);
        if (dto.name() != null && !dto.name().isBlank()) member.setName(dto.name());
        if (dto.phone() != null) member.setPhone(dto.phone());
        return toProfileDto(memberRepository.save(member));
    }

    public MemberProfileDto updatePayment(Long id, PaymentUpdateDto dto) {
        Member member = findOrThrow(id);
        member.setPaymentMethod(dto.paymentMethod());
        if (dto.cardNumber() != null && !dto.cardNumber().isBlank()) {
            member.setCardNumber(cardEncryptionService.encrypt(dto.cardNumber()));
        }
        member.setCardExpiryDate(dto.cardExpiryDate());
        member.setStreetAddress(dto.streetAddress());
        member.setAptUnit(dto.aptUnit());
        member.setCity(dto.city());
        member.setState(dto.state());
        member.setZipCode(dto.zipCode());
        return toProfileDto(memberRepository.save(member));
    }

    public void delete(Long id) {
        Member member = findOrThrow(id);
        appUserRepository.findByEmail(member.getEmail()).ifPresent(appUserRepository::delete);
        memberRepository.deleteById(id);
    }

    private Member findOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found: " + id));
    }

    private MemberStatus parseStatus(String status) {
        try {
            return MemberStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
    }

    private MembershipPlan resolvePlan(Long planId) {
        if (planId == null) return null;
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found: " + planId));
    }

    private LocalDate computeNextPaymentDate(Member member) {
        if (member.getMembershipPlan() == null || member.getJoinDate() == null) return null;
        int months = member.getMembershipPlan().getDurationMonths();
        LocalDate next = member.getJoinDate();
        LocalDate today = LocalDate.now();
        while (!next.isAfter(today)) {
            next = next.plusMonths(months);
        }
        return next;
    }

    public MemberDto toDto(Member m) {
        return new MemberDto(
                m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getJoinDate(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getId() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getName() : null
        );
    }

    private AdminMemberDto toAdminDto(Member m) {
        return new AdminMemberDto(
                m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getJoinDate(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getId() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getName() : null,
                m.getPaymentMethod(), cardEncryptionService.mask(m.getCardNumber()), m.getCardExpiryDate(),
                m.getStreetAddress(), m.getAptUnit(), m.getCity(), m.getState(), m.getZipCode(),
                computeNextPaymentDate(m)
        );
    }

    private MemberProfileDto toProfileDto(Member m) {
        return new MemberProfileDto(
                m.getId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getId() : null,
                m.getMembershipPlan() != null ? m.getMembershipPlan().getName() : null,
                computeNextPaymentDate(m),
                m.getPaymentMethod(), cardEncryptionService.mask(m.getCardNumber()), m.getCardExpiryDate(),
                m.getStreetAddress(), m.getAptUnit(), m.getCity(), m.getState(), m.getZipCode()
        );
    }
}
