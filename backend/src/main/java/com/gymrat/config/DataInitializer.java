package com.gymrat.config;

import com.gymrat.entity.AppUser;
import com.gymrat.entity.UserRole;
import com.gymrat.repository.AppUserRepository;
import com.gymrat.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // Seeded admin credential is env-driven so the prod password isn't in git.
    // Dev/local has a non-secret default; prod requires ADMIN_SEED_PASSWORD via Secret Manager.
    @Value("${app.seed.admin-password}")
    private String adminSeedPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminUser("admin@gym.com", adminSeedPassword);
        linkMemberUser("alex@example.com", "Member123!");
        linkMemberUser("jordan@example.com", "Member123!");
        linkMemberUser("sam@example.com", "Member123!");
    }

    private void createAdminUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) return;
        userRepository.save(AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .build());
    }

    private void linkMemberUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) return;
        memberRepository.findByEmail(email).ifPresent(member ->
                userRepository.save(AppUser.builder()
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.MEMBER)
                        .member(member)
                        .build())
        );
    }
}
