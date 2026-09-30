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
import java.util.UUID;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
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

    public UUID getTenantId() {
        try {
            return (contract != null && contract.getTenant() != null)
                    ? contract.getTenant().getId()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getTenantName() {
        try {
            return (contract != null && contract.getTenant() != null)
                    ? contract.getTenant().getFullName()
                    : "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getTenantPhone() {
        try {
            if (contract != null && contract.getTenant() != null) {
                String phone = contract.getTenant().getPhone();
                return (phone != null && !phone.isBlank()) ? phone : "N/A";
            }
        } catch (Exception e) {
            return "N/A";
        }
        return "N/A";
    }

    public String getTenantEmail() {
        try {
            if (contract != null && contract.getTenant() != null && contract.getTenant().getUser() != null) {
                String email = contract.getTenant().getUser().getUsername();
                return (email != null && !email.isBlank()) ? email : "N/A";
            }
        } catch (Exception e) {
            return "N/A";
        }
        return "N/A";
    }

    public String getRoomNumber() {
        try {
            return (contract != null && contract.getRoom() != null)
                    ? contract.getRoom().getRoomNumber()
                    : "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getFloor() {
        try {
            return (contract != null && contract.getRoom() != null)
                    ? String.valueOf(contract.getRoom().getFloor())
                    : "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }
}
