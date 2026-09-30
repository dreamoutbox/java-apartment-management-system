package com.dreamoutbox.ams.contract;

import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.room.RoomStatus;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private ContractService contractService;

    private Room room;
    private Tenant tenant;
    private Contract activeContract;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        room = Room.builder()
                .roomNumber("101")
                .floor("1")
                .baseRent(new BigDecimal("1000.00"))
                .status(RoomStatus.OCCUPIED)
                .build();

        tenant = Tenant.builder()
                .fullName("John Doe")
                .phone("0812345678")
                .idCardNumber("ID-123")
                .build();

        activeContract = Contract.builder()
                .room(room)
                .tenant(tenant)
                .monthlyRent(new BigDecimal("1000.00"))
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(ContractStatus.ACTIVE)
                .build();
    }

    @Test
    void testDeleteContractSoftDeletesAndFreesRoom() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(activeContract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        contractService.deleteContract(contractId);

        assertTrue(activeContract.isDeleted());
        assertNotNull(activeContract.getDeletedAt());
        assertEquals(ContractStatus.TERMINATED, activeContract.getStatus());
        verify(roomService).transition(room, RoomStatus.VACANT);
        verify(contractRepository).save(activeContract);
        verify(contractRepository, never()).delete(any());
    }

    @Test
    void testGetAllContractsReturnsAllContracts() {
        when(contractRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(activeContract));

        List<Contract> result = contractService.getAllContracts();

        assertEquals(1, result.size());
        verify(contractRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testGetAllActiveContractsExcludesSoftDeleted() {
        when(contractRepository.findByStatusAndDeletedFalse(ContractStatus.ACTIVE))
                .thenReturn(List.of(activeContract));

        List<Contract> result = contractService.getAllActiveContracts();

        assertEquals(1, result.size());
        verify(contractRepository).findByStatusAndDeletedFalse(ContractStatus.ACTIVE);
    }
}
