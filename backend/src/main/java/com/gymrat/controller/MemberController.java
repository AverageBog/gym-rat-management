package com.gymrat.controller;

import com.gymrat.dto.*;
import com.gymrat.security.AuthenticatedUser;
import com.gymrat.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDto> getAll(@RequestParam(required = false) String status) {
        return service.getAll(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, Authentication auth) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        if ("ADMIN".equals(user.role())) {
            return ResponseEntity.ok(service.getAdminDetail(id));
        }
        if (!id.equals(user.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return ResponseEntity.ok(service.getMemberProfile(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminMemberDto> create(@RequestBody MemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminMemberDto update(@PathVariable Long id, @RequestBody MemberDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public MemberDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.updateStatus(id, body.get("status"));
    }

    @PutMapping("/{id}/contact")
    public MemberProfileDto updateContact(@PathVariable Long id,
                                          @RequestBody ContactUpdateDto dto,
                                          Authentication auth) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        if ("MEMBER".equals(user.role()) && !id.equals(user.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return service.updateContact(id, dto);
    }

    @PutMapping("/{id}/payment")
    public MemberProfileDto updatePayment(@PathVariable Long id,
                                          @RequestBody PaymentUpdateDto dto,
                                          Authentication auth) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        if ("MEMBER".equals(user.role()) && !id.equals(user.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return service.updatePayment(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
