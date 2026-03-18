package com.gymrat.service;

import com.gymrat.dto.MemberDto;
import com.gymrat.entity.Member;
import com.gymrat.entity.MemberStatus;
import com.gymrat.entity.MembershipPlan;
import com.gymrat.repository.MemberRepository;
import com.gymrat.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    public List<MemberDto> getAll(String status) {
        if (status != null && !status.isBlank()) {
            MemberStatus memberStatus = parseStatus(status);
            return memberRepository.findByStatus(memberStatus).stream().map(this::toDto).toList();
        }
        return memberRepository.findAll().stream().map(this::toDto).toList();
    }

    public MemberDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    public MemberDto create(MemberDto dto) {
        memberRepository.findByEmail(dto.email()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + dto.email());
        });

        MembershipPlan plan = resolvePlan(dto.membershipPlanId());

        Member member = Member.builder()
                .name(dto.name())
                .email(dto.email())
                .phone(dto.phone())
                .joinDate(dto.joinDate() != null ? dto.joinDate() : LocalDate.now())
                .status(dto.status() != null ? parseStatus(dto.status()) : MemberStatus.ACTIVE)
                .membershipPlan(plan)
                .build();

        return toDto(memberRepository.save(member));
    }

    public MemberDto update(Long id, MemberDto dto) {
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

        return toDto(memberRepository.save(member));
    }

    public MemberDto updateStatus(Long id, String status) {
        Member member = findOrThrow(id);
        member.setStatus(parseStatus(status));
        return toDto(memberRepository.save(member));
    }

    public void delete(Long id) {
        findOrThrow(id);
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

    public MemberDto toDto(Member member) {
        return new MemberDto(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getJoinDate(),
                member.getStatus() != null ? member.getStatus().name() : null,
                member.getMembershipPlan() != null ? member.getMembershipPlan().getId() : null,
                member.getMembershipPlan() != null ? member.getMembershipPlan().getName() : null
        );
    }
}
