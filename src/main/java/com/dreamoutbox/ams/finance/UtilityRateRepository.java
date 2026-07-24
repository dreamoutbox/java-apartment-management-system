package com.dreamoutbox.ams.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UtilityRateRepository extends JpaRepository<UtilityRate, UUID> {
    Optional<UtilityRate> findByPeriod(String period);
}
