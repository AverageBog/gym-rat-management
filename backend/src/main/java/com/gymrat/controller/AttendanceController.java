package com.gymrat.controller;

import com.gymrat.dto.AttendanceDto;
import com.gymrat.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping("/checkin/{memberId}")
    public ResponseEntity<AttendanceDto> checkIn(@PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.checkIn(memberId));
    }

    @GetMapping("/member/{memberId}")
    public List<AttendanceDto> getByMember(@PathVariable Long memberId) {
        return service.getByMember(memberId);
    }

    @GetMapping
    public List<AttendanceDto> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.getAll(start, end);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
