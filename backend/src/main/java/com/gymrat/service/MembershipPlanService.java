package com.gymrat.service;

import com.gymrat.dto.MembershipPlanDto;
import com.gymrat.entity.MembershipPlan;
import com.gymrat.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository repository;

    public List<MembershipPlanDto> getAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public MembershipPlanDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    public MembershipPlanDto create(MembershipPlanDto dto) {
        MembershipPlan plan = MembershipPlan.builder()
                .name(dto.name())
                .durationMonths(dto.durationMonths())
                .price(dto.price())
                .description(dto.description())
                .build();
        return toDto(repository.save(plan));
    }

    public MembershipPlanDto update(Long id, MembershipPlanDto dto) {
        MembershipPlan plan = findOrThrow(id);
        plan.setName(dto.name());
        plan.setDurationMonths(dto.durationMonths());
        plan.setPrice(dto.price());
        plan.setDescription(dto.description());
        return toDto(repository.save(plan));
    }

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private MembershipPlan findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found: " + id));
    }

    public MembershipPlanDto toDto(MembershipPlan plan) {
        return new MembershipPlanDto(plan.getId(), plan.getName(), plan.getDurationMonths(), plan.getPrice(), plan.getDescription());
    }
}
