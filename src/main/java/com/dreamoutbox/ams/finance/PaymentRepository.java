package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByTenantOrderByPaidAtDesc(Tenant tenant);
    List<Payment> findAllByOrderByPaidAtDesc();
}
