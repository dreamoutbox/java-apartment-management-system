package com.dreamoutbox.ams.config;

import com.dreamoutbox.ams.announcement.AnnouncementService;
import com.dreamoutbox.ams.announcement.AnnouncementType;
import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomRepository;
import com.dreamoutbox.ams.room.RoomStatus;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserRepository;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final MaintenanceService maintenanceService;
    private final AnnouncementService announcementService;

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
            String floor = String.valueOf(floorNum);
            BigDecimal baseRent = BigDecimal.valueOf(1000 + (roomNumInFloor - 1) * 100);
            createOrUpdateRoom(roomNumber, floor, baseRent);
        }

        log.info("Default seed rooms ready.");

        seedTenantData();
    }

    private void seedTenantData() {
        try {
            User landlord = userRepository.findByUsername("landlord").orElse(null);
            User tenantUser1 = userRepository.findByUsername("tenant1").orElse(null);
            Room room101 = roomRepository.findByRoomNumber("101").orElse(null);

            if (tenantUser1 != null && room101 != null) {
                Tenant tenant1 = tenantRepository.findByUserId(tenantUser1.getId())
                        .orElseGet(() -> tenantRepository.save(Tenant.builder()
                                .user(tenantUser1)
                                .fullName(tenantUser1.getFullName())
                                .phone("0812345678")
                                .idCardNumber("ID-tenant1")
                                .creditBalance(BigDecimal.ZERO)
                                .build()));

                if (contractService.getTenantActiveContracts(tenant1).isEmpty()) {
                    contractService.createContract(
                            room101.getId(),
                            tenant1.getId(),
                            BigDecimal.valueOf(1200),
                            LocalDate.now().minusMonths(2),
                            LocalDate.now().plusMonths(10)
                    );
                    log.info("Seeded lease contract for tenant1 in Room 101");
                }

                // Seed meter reading and invoices if needed
                try {
                    invoiceService.setUtilityRate("2026-07", BigDecimal.valueOf(0.25), BigDecimal.valueOf(1.50));
                    invoiceService.recordMeterReading(room101, "2026-07", BigDecimal.valueOf(1250), BigDecimal.valueOf(340));
                    invoiceService.generateMonthlyInvoices("2026-07");
                } catch (Exception ignored) {}

                // Seed announcement if none exist
                if (announcementService.getAllAnnouncements().isEmpty() && landlord != null) {
                    announcementService.createAnnouncement(
                            "Welcome to Apartment Hub Portal",
                            "Dear Residents, welcome to our online portal. You can now track your monthly invoices, submit maintenance tickets, and check building announcements right here.",
                            AnnouncementType.NEWS,
                            true,
                            landlord
                    );
                    announcementService.createAnnouncement(
                            "Building Rules & Regulations",
                            "Please observe quiet hours between 10:00 PM and 7:00 AM.",
                            AnnouncementType.RULE,
                            false,
                            landlord
                    );
                }

                // Seed maintenance ticket if none exist
                if (maintenanceService.getTenantTickets(tenant1).isEmpty()) {
                    maintenanceService.createTicket(room101, tenant1, "Air conditioner in living room is not cooling properly.", null);
                }
            }
        } catch (Exception e) {
            log.warn("Error seeding tenant data: {}", e.getMessage());
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
