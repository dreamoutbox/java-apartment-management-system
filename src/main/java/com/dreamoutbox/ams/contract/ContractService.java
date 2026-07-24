package com.dreamoutbox.ams.contract;

import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.room.RoomStatus;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomService roomService;
    private final TenantService tenantService;

    @Value("${app.lease.renewal-warning-days:30}")
    private int renewalWarningDays;

    @Transactional
    public Contract createContract(UUID roomId, UUID tenantId, BigDecimal monthlyRent, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Contract end date cannot be before start date");
        }

        Room room = roomService.getById(roomId);
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalStateException("Cannot create contract for room currently in maintenance");
        }

        Tenant tenant = tenantService.getById(tenantId);

        List<Contract> overlapping = contractRepository.findOverlappingActiveContracts(room, startDate, endDate);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " already has an active contract for the given period");
        }

        // Transition room to OCCUPIED
        roomService.transition(room, RoomStatus.OCCUPIED);

        Contract contract = Contract.builder()
                .room(room)
                .tenant(tenant)
                .monthlyRent(monthlyRent != null ? monthlyRent : room.getBaseRent())
                .startDate(startDate)
                .endDate(endDate)
                .status(ContractStatus.ACTIVE)
                .build();

        return contractRepository.save(contract);
    }

    @Transactional
    public Contract terminateContract(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new IllegalStateException("Contract is not currently active");
        }

        contract.setStatus(ContractStatus.TERMINATED);
        roomService.transition(contract.getRoom(), RoomStatus.VACANT);
        return contractRepository.save(contract);
    }

    @Transactional(readOnly = true)
    public List<Contract> getExpiringContracts() {
        LocalDate thresholdDate = LocalDate.now().plusDays(renewalWarningDays);
        return contractRepository.findExpiringContracts(thresholdDate);
    }

    @Transactional(readOnly = true)
    public List<Contract> getAllActiveContracts() {
        return contractRepository.findByStatus(ContractStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Contract> getTenantActiveContracts(Tenant tenant) {
        return contractRepository.findByTenantAndStatus(tenant, ContractStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Contract getById(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + id));
    }
}
