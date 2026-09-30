package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.config.DataSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Slf4j
public class DevMockController {

    private final DataSeederService dataSeederService;

    @PostMapping("/mock-data")
    public ResponseEntity<String> mockData() {
        log.info("Triggered mock data seeding via /api/dev/mock-data");
        var systemAuth = new UsernamePasswordAuthenticationToken(
                "system", null, List.of(
                        new SimpleGrantedAuthority("ROLE_LANDLORD"),
                        new SimpleGrantedAuthority("ROLE_MANAGER"),
                        new SimpleGrantedAuthority("ROLE_ACCOUNTANT"),
                        new SimpleGrantedAuthority("ROLE_TECHNICIAN"),
                        new SimpleGrantedAuthority("ROLE_TENANT")));
        SecurityContextHolder.getContext().setAuthentication(systemAuth);
        try {
            dataSeederService.seed();
            return ResponseEntity.ok("Mock data seeded successfully.");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
