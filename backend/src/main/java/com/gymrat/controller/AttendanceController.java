package com.gymrat.controller;

import com.gymrat.dto.AttendanceDto;
import com.gymrat.security.AuthenticatedUser;
import com.gymrat.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping("/checkin/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceDto> checkIn(@PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.checkIn(memberId));
    }

    @GetMapping("/member/{memberId}")
    public List<AttendanceDto> getByMember(@PathVariable Long memberId, Authentication auth) {
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        if ("MEMBER".equals(user.role()) && !memberId.equals(user.memberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return service.getByMember(memberId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AttendanceDto> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.getAll(start, end);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
