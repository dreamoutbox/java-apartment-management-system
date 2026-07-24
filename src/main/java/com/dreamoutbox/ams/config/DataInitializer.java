package com.dreamoutbox.ams.config;

import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Ensuring default seed users exist and are valid...");
        createOrUpdateUser("landlord", "password", "System Landlord", Role.LANDLORD);
        createOrUpdateUser("manager", "password", "System Manager", Role.MANAGER);
        createOrUpdateUser("accountant", "password", "System Accountant", Role.ACCOUNTANT);
        createOrUpdateUser("technician", "password", "System Technician", Role.TECHNICIAN);
        createOrUpdateUser("tenant", "password", "System Tenant", Role.TENANT);
        log.info("Default seed users ready.");
    }

    private void createOrUpdateUser(String username, String rawPassword, String fullName, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    user.setFullName(fullName);
                    user.setRole(role);
                    user.setEnabled(true);
                    userRepository.save(user);
                },
                () -> userService.createUser(username, rawPassword, fullName, role));
    }
}
