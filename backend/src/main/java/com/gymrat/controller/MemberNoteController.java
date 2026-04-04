package com.gymrat.controller;

import com.gymrat.dto.MemberNoteDto;
import com.gymrat.service.MemberNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MemberNoteController {

    private final MemberNoteService service;

    @GetMapping("/member/{memberId}")
    public List<MemberNoteDto> getByMember(@PathVariable Long memberId) {
        return service.getByMember(memberId);
    }

    @PostMapping("/member/{memberId}")
    public ResponseEntity<MemberNoteDto> addNote(@PathVariable Long memberId, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addNote(memberId, body.get("content")));
    }

    @PutMapping("/{id}")
    public MemberNoteDto updateNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.updateNote(id, body.get("content"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        service.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
