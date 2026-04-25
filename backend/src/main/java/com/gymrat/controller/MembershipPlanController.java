package com.gymrat.controller;

import com.gymrat.dto.MembershipPlanDto;
import com.gymrat.service.MembershipPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MembershipPlanController {

    private final MembershipPlanService service;

    @GetMapping
    public List<MembershipPlanDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MembershipPlanDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<MembershipPlanDto> create(@RequestBody MembershipPlanDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public MembershipPlanDto update(@PathVariable Long id, @RequestBody MembershipPlanDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
