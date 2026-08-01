package com.dreamoutbox.ams.config;

import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomRepository;
import com.dreamoutbox.ams.room.RoomStatus;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;
    private final DataSeederService dataSeederService;

    @Override
    public void run(String... args) {
        log.info("Ensuring default seed users exist and are valid...");
        createOrUpdateUser("landlord", "password", "System Landlord", Role.LANDLORD);
        createOrUpdateUser("manager", "password", "System Manager", Role.MANAGER);
        createOrUpdateUser("accountant", "password", "System Accountant", Role.ACCOUNTANT);
        createOrUpdateUser("technician", "password", "System Technician", Role.TECHNICIAN);
        for (int i = 1; i <= 20; i++) {
            createOrUpdateUser("tenant" + i, "password", "System Tenant " + i, Role.TENANT);
        }
        log.info("Default seed users ready.");

        log.info("Ensuring default seed rooms exist...");
        for (int i = 1; i <= 20; i++) {
            int floorNum = (i - 1) / 5 + 1;
            int roomNumInFloor = (i - 1) % 5 + 1;
            String roomNumber = String.format("%d%02d", floorNum, roomNumInFloor);
            BigDecimal baseRent = BigDecimal.valueOf(1000 + (roomNumInFloor - 1) * 100);
            createOrUpdateRoom(roomNumber, String.valueOf(floorNum), baseRent);
        }
        log.info("Default seed rooms ready.");

        // Provide a system authentication so @CreatedBy auditing and @PreAuthorize
        // checks pass
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
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    private void createOrUpdateRoom(String roomNumber, String floor, BigDecimal baseRent) {
        roomRepository.findByRoomNumber(roomNumber).ifPresentOrElse(
                room -> {
                    room.setFloor(floor);
                    room.setBaseRent(baseRent);
                    roomRepository.save(room);
                },
                () -> roomRepository.save(Room.builder()
                        .roomNumber(roomNumber)
                        .floor(floor)
                        .baseRent(baseRent)
                        .status(RoomStatus.VACANT)
                        .build()));
    }
}
