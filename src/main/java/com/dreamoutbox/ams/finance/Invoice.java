package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.common.BaseEntity;
import com.dreamoutbox.ams.contract.Contract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "contract_id", "period" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private String period; // Format: "YYYY-MM"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal electricityAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal waterAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceRemaining;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    public String getInvoiceNumber() {
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            return invoiceNumber;
        }
        String ym = (period != null && !period.isBlank()) ? period.replace("-", "") : "202607";
        return "INV-" + ym + "-00001";
    }
}
