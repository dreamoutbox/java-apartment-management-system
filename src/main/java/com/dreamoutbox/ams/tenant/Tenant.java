package com.dreamoutbox.ams.tenant;

import com.dreamoutbox.ams.common.BaseEntity;
import com.dreamoutbox.ams.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false)
    private String idCardNumber;

    private String idCardImagePath;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal creditBalance = BigDecimal.ZERO;
}
