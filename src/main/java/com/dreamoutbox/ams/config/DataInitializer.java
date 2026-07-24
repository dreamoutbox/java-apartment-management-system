package com.dreamoutbox.ams.config;

import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial system users...");
            userService.createUser("landlord", "password", "System Landlord", Role.LANDLORD);
            userService.createUser("manager", "password", "System Manager", Role.MANAGER);
            userService.createUser("accountant", "password", "System Accountant", Role.ACCOUNTANT);
            userService.createUser("technician", "password", "System Technician", Role.TECHNICIAN);
            userService.createUser("tenant", "password", "System Tenant", Role.TENANT);
            log.info("Default users created with password 'password'.");
        }
    }
}
