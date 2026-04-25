package com.gymrat.service;

import com.gymrat.dto.CreateAdminRequest;
import com.gymrat.dto.LoginRequest;
import com.gymrat.dto.LoginResponse;
import com.gymrat.entity.AppUser;
import com.gymrat.entity.UserRole;
import com.gymrat.repository.AppUserRepository;
import com.gymrat.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest req) {
        AppUser user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtUtil.generate(user);
        String name = user.getMember() != null ? user.getMember().getName() : user.getEmail();
        Long memberId = user.getMember() != null ? user.getMember().getId() : null;

        return new LoginResponse(token, user.getRole().name(), memberId, user.getEmail(), name);
    }

    public void createAdmin(CreateAdminRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use: " + req.email());
        }
        if (req.password() == null || req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        AppUser admin = AppUser.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);
    }
}
