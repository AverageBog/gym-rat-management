package com.gymrat.controller;

import com.gymrat.dto.MerchandiseItemDto;
import com.gymrat.service.MerchandiseItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchandise")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MerchandiseItemController {

    private final MerchandiseItemService service;

    @GetMapping
    public List<MerchandiseItemDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public MerchandiseItemDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<MerchandiseItemDto> create(@RequestBody MerchandiseItemDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public MerchandiseItemDto update(@PathVariable Long id, @RequestBody MerchandiseItemDto dto) {
        return service.update(id, dto);
    }

    @PatchMapping("/{id}/quantity")
    public MerchandiseItemDto adjustQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return service.adjustQuantity(id, body.get("delta"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
