package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.common.DocumentSequenceService;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import com.dreamoutbox.ams.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final DocumentSequenceService documentSequenceService;

    @Transactional
    public Payment recordPayment(Tenant tenant, BigDecimal amount, User receivedBy) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        String ymStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String nextRecNum = documentSequenceService.getNextNumber("REC", ymStr);

        Payment payment = Payment.builder()
                .receiptNumber(nextRecNum)
                .tenant(tenant)
                .amount(amount)
                .paidAt(LocalDateTime.now())
                .receivedBy(receivedBy)
                .build();

        BigDecimal remaining = amount
                .add(tenant.getCreditBalance() != null ? tenant.getCreditBalance() : BigDecimal.ZERO);
        tenant.setCreditBalance(BigDecimal.ZERO);

        List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidInvoicesForTenantForUpdate(
                tenant, List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID));

        for (Invoice invoice : unpaidInvoices) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal owed = invoice.getBalanceRemaining();
            BigDecimal applied = remaining.min(owed);

            PaymentAllocation allocation = PaymentAllocation.builder()
                    .payment(payment)
                    .invoice(invoice)
                    .amountApplied(applied)
                    .build();

            payment.getAllocations().add(allocation);

            BigDecimal newBalance = owed.subtract(applied);
            invoice.setBalanceRemaining(newBalance);
            invoice.setStatus(newBalance.compareTo(BigDecimal.ZERO) == 0
                    ? InvoiceStatus.PAID
                    : InvoiceStatus.PARTIALLY_PAID);

            invoiceRepository.save(invoice);
            remaining = remaining.subtract(applied);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // Overpayment stored as credit balance for tenant
            tenant.setCreditBalance(remaining);
        }

        tenantRepository.save(tenant);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByTenant(Tenant tenant) {
        return paymentRepository.findByTenantOrderByPaidAtDesc(tenant);
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaidAtDesc();
    }

    @Transactional(readOnly = true)
    public Payment getById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingReceiptNumbers() {
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtAsc();
        for (Payment p : payments) {
            if (p.getReceiptNumber() == null || p.getReceiptNumber().isBlank()) {
                String ymStr = (p.getPaidAt() != null)
                        ? p.getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"))
                        : "202607";
                p.setReceiptNumber(documentSequenceService.getNextNumber("REC", ymStr));
                paymentRepository.save(p);
            }
        }
    }
}
