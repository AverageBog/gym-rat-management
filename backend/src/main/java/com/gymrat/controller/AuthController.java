package com.gymrat.controller;

import com.gymrat.dto.CreateAdminRequest;
import com.gymrat.dto.LoginRequest;
import com.gymrat.dto.LoginResponse;
import com.gymrat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createAdmin(@RequestBody CreateAdminRequest req) {
        authService.createAdmin(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
