package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.common.BaseEntity;
import com.dreamoutbox.ams.room.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "meter_reading", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "period"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String period; // Format: "YYYY-MM"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal electricityReading;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal waterReading;
}
