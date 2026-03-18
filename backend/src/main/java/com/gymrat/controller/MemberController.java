package com.gymrat.controller;

import com.gymrat.dto.MemberDto;
import com.gymrat.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @GetMapping
    public List<MemberDto> getAll(@RequestParam(required = false) String status) {
        return service.getAll(status);
    }

    @GetMapping("/{id}")
    public MemberDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<MemberDto> create(@RequestBody MemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public MemberDto update(@PathVariable Long id, @RequestBody MemberDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/status")
    public MemberDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.updateStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
