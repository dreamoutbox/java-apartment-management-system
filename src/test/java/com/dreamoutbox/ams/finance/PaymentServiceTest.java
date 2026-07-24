package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomStatus;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private com.dreamoutbox.ams.tenant.TenantRepository tenantRepository;

    @InjectMocks
    private PaymentService paymentService;

    private User accountant;
    private Tenant bob;
    private Contract contract;

    @BeforeEach
    void setUp() {
        accountant = User.builder()
                .username("accountant1")
                .passwordHash("hash")
                .role(Role.ACCOUNTANT)
                .build();
        accountant.setId(UUID.randomUUID());

        bob = Tenant.builder()
                .fullName("Bob")
                .phone("12345678")
                .idCardNumber("ID123")
                .creditBalance(BigDecimal.ZERO)
                .build();
        bob.setId(UUID.randomUUID());

        Room room = Room.builder()
                .roomNumber("101")
                .floor("1")
                .baseRent(new BigDecimal("3000.00"))
                .status(RoomStatus.OCCUPIED)
                .build();

        contract = Contract.builder()
                .room(room)
                .tenant(bob)
                .monthlyRent(new BigDecimal("3000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
    }

    @Test
    void testBobPartialPaymentThenFullPaymentScenario() {
        // Scenario Step 1: Jan Invoice 3000. Bob pays 2000.
        Invoice janInvoice = Invoice.builder()
                .contract(contract)
                .period("2026-01")
                .rentAmount(new BigDecimal("3000.00"))
                .electricityAmount(BigDecimal.ZERO)
                .waterAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("3000.00"))
                .balanceRemaining(new BigDecimal("3000.00"))
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDate.of(2026, 1, 10))
                .build();

        when(invoiceRepository.findUnpaidInvoicesForTenantForUpdate(eq(bob), any()))
                .thenReturn(List.of(janInvoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment1 = paymentService.recordPayment(bob, new BigDecimal("2000.00"), accountant);

        // Verification 1: Jan Invoice balance becomes 1000, status PARTIALLY_PAID
        assertEquals(1, payment1.getAllocations().size());
        assertEquals(new BigDecimal("2000.00"), payment1.getAllocations().get(0).getAmountApplied());
        assertEquals(new BigDecimal("1000.00"), janInvoice.getBalanceRemaining());
        assertEquals(InvoiceStatus.PARTIALLY_PAID, janInvoice.getStatus());

        // Scenario Step 2: Feb Invoice 3000 generated. Unpaid invoices = [Jan (1000 balance), Feb (3000 balance)].
        Invoice febInvoice = Invoice.builder()
                .contract(contract)
                .period("2026-02")
                .rentAmount(new BigDecimal("3000.00"))
                .electricityAmount(BigDecimal.ZERO)
                .waterAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("3000.00"))
                .balanceRemaining(new BigDecimal("3000.00"))
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDate.of(2026, 2, 10))
                .build();

        List<Invoice> unpaidInvoices = new ArrayList<>();
        unpaidInvoices.add(janInvoice);
        unpaidInvoices.add(febInvoice);

        when(invoiceRepository.findUnpaidInvoicesForTenantForUpdate(eq(bob), any()))
                .thenReturn(unpaidInvoices);

        // Bob pays 4000
        Payment payment2 = paymentService.recordPayment(bob, new BigDecimal("4000.00"), accountant);

        // Verification 2: FIFO Allocation: 1000 to Jan (making Jan PAID), 3000 to Feb (making Feb PAID)
        assertEquals(2, payment2.getAllocations().size());
        assertEquals(new BigDecimal("1000.00"), payment2.getAllocations().get(0).getAmountApplied());
        assertEquals(new BigDecimal("3000.00"), payment2.getAllocations().get(1).getAmountApplied());

        assertEquals(0, janInvoice.getBalanceRemaining().compareTo(BigDecimal.ZERO));
        assertEquals(InvoiceStatus.PAID, janInvoice.getStatus());

        assertEquals(0, febInvoice.getBalanceRemaining().compareTo(BigDecimal.ZERO));
        assertEquals(InvoiceStatus.PAID, febInvoice.getStatus());
        assertEquals(0, bob.getCreditBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testOverpaymentRecordedAsCreditBalance() {
        Invoice invoice = Invoice.builder()
                .contract(contract)
                .period("2026-01")
                .rentAmount(new BigDecimal("1000.00"))
                .electricityAmount(BigDecimal.ZERO)
                .waterAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .balanceRemaining(new BigDecimal("1000.00"))
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDate.of(2026, 1, 10))
                .build();

        when(invoiceRepository.findUnpaidInvoicesForTenantForUpdate(eq(bob), any()))
                .thenReturn(List.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = paymentService.recordPayment(bob, new BigDecimal("1500.00"), accountant);

        assertNotNull(payment);
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(0, invoice.getBalanceRemaining().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("500.00"), bob.getCreditBalance());
    }
}
