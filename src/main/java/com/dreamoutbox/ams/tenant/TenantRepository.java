package com.dreamoutbox.ams.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByUserId(UUID userId);
    Optional<Tenant> findByIdCardNumber(String idCardNumber);
}
