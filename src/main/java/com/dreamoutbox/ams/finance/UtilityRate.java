package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "utility_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityRate extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String period; // Format: "YYYY-MM" (YearMonth)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal electricityUnitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal waterUnitPrice;
}
