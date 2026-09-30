package com.dreamoutbox.ams.contract;

import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByStatus(ContractStatus status);

    List<Contract> findByStatusAndDeletedFalse(ContractStatus status);

    List<Contract> findByTenantAndStatus(Tenant tenant, ContractStatus status);

    List<Contract> findByTenantAndStatusAndDeletedFalse(Tenant tenant, ContractStatus status);

    Optional<Contract> findFirstByRoomAndStatus(Room room, ContractStatus status);

    List<Contract> findByRoom(Room room);

    List<Contract> findByRoomAndDeletedFalse(Room room);

    List<Contract> findAllByDeletedFalseOrderByCreatedAtDesc();

    List<Contract> findAllByOrderByCreatedAtDesc();

    @Query("SELECT c FROM Contract c WHERE c.room = :room AND c.status = 'ACTIVE' AND c.deleted = false "
            + "AND (:startDate <= c.endDate AND :endDate >= c.startDate)")
    List<Contract> findOverlappingActiveContracts(
            @Param("room") Room room,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' AND c.deleted = false AND c.endDate <= :thresholdDate")
    List<Contract> findExpiringContracts(@Param("thresholdDate") LocalDate thresholdDate);
}
