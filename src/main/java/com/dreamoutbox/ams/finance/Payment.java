package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.common.BaseEntity;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id", nullable = false)
    private User receivedBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentAllocation> allocations = new ArrayList<>();

    @Column(name = "receipt_number")
    private String receiptNumber;

    public String getReceiptNumber() {
        if (receiptNumber != null && !receiptNumber.isBlank()) {
            return receiptNumber;
        }
        String ym = (paidAt != null) ? paidAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) : "202607";
        return "REC-" + ym + "-00001";
    }
}
