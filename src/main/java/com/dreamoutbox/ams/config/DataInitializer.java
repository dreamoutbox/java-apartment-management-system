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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        log.info("Ensuring default seed users exist and are valid...");
        createOrUpdateUser("landlord", "password", "System Landlord", Role.LANDLORD);
        createOrUpdateUser("manager", "password", "System Manager", Role.MANAGER);
        createOrUpdateUser("accountant", "password", "System Accountant", Role.ACCOUNTANT);
        createOrUpdateUser("technician", "password", "System Technician", Role.TECHNICIAN);
        createOrUpdateUser("tenant", "password", "System Tenant", Role.TENANT);

        for (int i = 1; i <= 20; i++) {
            createOrUpdateUser("tenant" + i, "password", "System Tenant " + i, Role.TENANT);
        }

        log.info("Default seed users ready.");

        log.info("Ensuring default seed rooms exist...");
        for (int i = 1; i <= 20; i++) {
            int floorNum = (i - 1) / 5 + 1;
            int roomNumInFloor = (i - 1) % 5 + 1;
            String roomNumber = String.format("%d%02d", floorNum, roomNumInFloor);
            String floor = String.valueOf(floorNum);
            BigDecimal baseRent = BigDecimal.valueOf(1000 + (roomNumInFloor - 1) * 100);
            createOrUpdateRoom(roomNumber, floor, baseRent);
        }

        log.info("Default seed rooms ready.");
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
                () -> {
                    Room room = Room.builder()
                            .roomNumber(roomNumber)
                            .floor(floor)
                            .baseRent(baseRent)
                            .status(RoomStatus.VACANT)
                            .build();
                    roomRepository.save(room);
                });
    }
}
