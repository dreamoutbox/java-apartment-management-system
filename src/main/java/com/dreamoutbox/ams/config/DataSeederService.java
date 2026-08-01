package com.dreamoutbox.ams.config;

import com.dreamoutbox.ams.announcement.AnnouncementService;
import com.dreamoutbox.ams.announcement.AnnouncementType;
import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.finance.PaymentService;
import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.maintenance.MaintenanceTicket;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomRepository;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs all demo/seed data operations inside a single @Transactional context
 * so that no entity becomes a detached proxy between calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeederService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final MaintenanceService maintenanceService;
    private final AnnouncementService announcementService;

    @Transactional
    public void seed() {
        try {
            User landlord = userRepository.findByUsername("landlord").orElse(null);
            User manager = userRepository.findByUsername("manager").orElse(null);
            User accountant = userRepository.findByUsername("accountant").orElse(null);
            User technician = userRepository.findByUsername("technician").orElse(null);

            // Rooms
            Room room101 = roomRepository.findByRoomNumber("101").orElse(null);
            Room room102 = roomRepository.findByRoomNumber("102").orElse(null);
            Room room103 = roomRepository.findByRoomNumber("103").orElse(null);
            Room room104 = roomRepository.findByRoomNumber("104").orElse(null);
            if (room101 == null || room102 == null || room103 == null || room104 == null) {
                log.warn("Rooms not found - skipping tenant seed");
                return;
            }

            // Tenants
            Tenant tenant1 = getOrCreateTenant("tenant1", "0812345671", "ID-tenant1");
            Tenant tenant2 = getOrCreateTenant("tenant2", "0812345672", "ID-tenant2");
            Tenant tenant3 = getOrCreateTenant("tenant3", "0812345673", "ID-tenant3");
            Tenant tenant4 = getOrCreateTenant("tenant4", "0812345674", "ID-tenant4");

            // Contracts: Jan-Dec 2026, rent = 1000
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 12, 31);
            BigDecimal rent = BigDecimal.valueOf(1000);

            Contract contract1 = ensureContract(tenant1, room101, rent, start, end);
            Contract contract2 = ensureContract(tenant2, room102, rent, start, end);
            Contract contract3 = ensureContract(tenant3, room103, rent, start, end);
            Contract contract4 = ensureContract(tenant4, room104, rent, start, end);

            // Utility rates 2026-01 to 2026-05 (elec/water: 1.0, 1.5, 2.0, 2.5, 3.0)
            String[] periods = { "2026-01", "2026-02", "2026-03", "2026-04", "2026-05" };
            double[] rates = { 1.0, 1.5, 2.0, 2.5, 3.0 };
            for (int i = 0; i < periods.length; i++) {
                if (invoiceService.getUtilityRate(periods[i]) == null) {
                    invoiceService.setUtilityRate(periods[i],
                            BigDecimal.valueOf(rates[i]), BigDecimal.valueOf(rates[i]));
                }
            }

            // Meter readings: cumulative 10, 20, 30, 40, 50 for elec + water per room
            Room[] rooms = { room101, room102, room103, room104 };
            for (Room room : rooms) {
                for (int i = 0; i < periods.length; i++) {
                    BigDecimal reading = BigDecimal.valueOf((long) (i + 1) * 10);
                    if (invoiceService.getMeterReading(room, periods[i]) == null) {
                        invoiceService.recordMeterReading(room, periods[i], reading, reading);
                    }
                }
            }

            // Generate invoices for all 4 contracts x 5 months
            Contract[] contracts = { contract1, contract2, contract3, contract4 };
            for (String period : periods) {
                for (Contract contract : contracts) {
                    invoiceService.generateInvoice(contract, period);
                }
            }

            // Payments
            if (accountant != null) {
                // Tenant 1: fully paid
                BigDecimal t1Owed = computeTotalOwed(tenant1);
                if (t1Owed.compareTo(BigDecimal.ZERO) > 0) {
                    paymentService.recordPayment(tenant1, t1Owed, accountant);
                    log.info("Tenant1: fully paid all invoices ({})", t1Owed);
                }
                // Tenant 2: pay months 01-04 in full
                BigDecimal t2Full = computeBalanceForPeriods(tenant2, "2026-01", "2026-02", "2026-03", "2026-04");
                if (t2Full.compareTo(BigDecimal.ZERO) > 0) {
                    paymentService.recordPayment(tenant2, t2Full, accountant);
                    log.info("Tenant2: paid months 01-04 ({})", t2Full);
                }
                // Tenant 2: partial on month 05
                BigDecimal t2Month5 = computeBalanceForPeriods(tenant2, "2026-05");
                if (t2Month5.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal partial = t2Month5.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                    paymentService.recordPayment(tenant2, partial, accountant);
                    log.info("Tenant2: partial payment on month 05 ({})", partial);
                }
                // Tenants 3 & 4: leave unpaid
            }

            // Announcements
            if (announcementService.getAllAnnouncements().isEmpty() && landlord != null) {
                announcementService.createAnnouncement(
                        "Welcome to Apartment Hub Portal",
                        "Dear Residents, welcome to our online portal. You can now track your monthly invoices, submit maintenance tickets, and check building announcements right here.",
                        AnnouncementType.NEWS, true, landlord);
                announcementService.createAnnouncement(
                        "Building Rules & Regulations",
                        "Please observe quiet hours between 10:00 PM and 7:00 AM.",
                        AnnouncementType.RULE, false, landlord);
            }

            // Maintenance ticket for tenant1 with demo image + assign technician
            List<MaintenanceTicket> tickets = maintenanceService.getTenantTickets(tenant1);
            if (tickets.isEmpty()) {
                MultipartFile demoImage = loadDemoImage("demo/images/image.png");
                MaintenanceTicket ticket = maintenanceService.createTicket(room101, tenant1,
                        "Air conditioner in living room is not cooling properly.", demoImage);
                log.info("Seeded maintenance ticket for tenant1 in Room 101");

                if (technician != null && manager != null) {
                    maintenanceService.assignTechnician(ticket.getId(), technician.getId(), manager);
                    log.info("Assigned technician '{}' to maintenance ticket", technician.getFullName());
                }
            }

            log.info("Seed data complete.");

        } catch (Exception e) {
            log.warn("Error seeding tenant data: {}", e.getMessage(), e);
        }
    }

    // Helpers

    private Tenant getOrCreateTenant(String username, String phone, String idCard) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return null;
        return tenantRepository.findByUserId(user.getId())
                .orElseGet(() -> tenantRepository.save(Tenant.builder()
                        .user(user)
                        .fullName(user.getFullName())
                        .phone(phone)
                        .idCardNumber(idCard)
                        .creditBalance(BigDecimal.ZERO)
                        .build()));
    }

    private Contract ensureContract(Tenant tenant, Room room, BigDecimal rent,
            LocalDate start, LocalDate end) {
        List<Contract> active = contractService.getTenantActiveContracts(tenant);
        if (!active.isEmpty())
            return active.get(0);
        Contract c = contractService.createContract(room.getId(), tenant.getId(), rent, start, end);
        log.info("Seeded contract: {} -> Room {}", tenant.getFullName(), room.getRoomNumber());
        return c;
    }

    private BigDecimal computeBalanceForPeriods(Tenant tenant, String... targetPeriods) {
        Set<String> wanted = new HashSet<>(Arrays.asList(targetPeriods));
        return invoiceService.getTenantInvoices(tenant).stream()
                .filter(inv -> wanted.contains(inv.getPeriod()))
                .map(inv -> inv.getBalanceRemaining() != null ? inv.getBalanceRemaining() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    private BigDecimal computeTotalOwed(Tenant tenant) {
        return invoiceService.getTenantInvoices(tenant).stream()
                .map(inv -> inv.getBalanceRemaining() != null ? inv.getBalanceRemaining() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    private MultipartFile loadDemoImage(String relativePath) {
        try {
            Path path = Paths.get(relativePath);
            if (!Files.exists(path)) {
                log.warn("Demo image not found at {}", path.toAbsolutePath());
                return null;
            }
            byte[] bytes = Files.readAllBytes(path);
            String filename = path.getFileName().toString();
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "reportedImage";
                }

                @Override
                public String getOriginalFilename() {
                    return filename;
                }

                @Override
                public String getContentType() {
                    return "image/png";
                }

                @Override
                public boolean isEmpty() {
                    return bytes.length == 0;
                }

                @Override
                public long getSize() {
                    return bytes.length;
                }

                @Override
                public byte[] getBytes() {
                    return bytes;
                }

                @Override
                public java.io.InputStream getInputStream() {
                    return new ByteArrayInputStream(bytes);
                }

                @Override
                public void transferTo(java.io.File dest) throws IOException {
                    Files.write(dest.toPath(), bytes);
                }
            };
        } catch (IOException e) {
            log.warn("Could not load demo image: {}", e.getMessage());
            return null;
        }
    }
}
