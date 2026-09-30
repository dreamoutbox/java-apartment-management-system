package com.dreamoutbox.ams.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.contract.ContractRepository;
import com.dreamoutbox.ams.contract.ContractStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        room = Room.builder()
                .roomNumber("101")
                .floor("1")
                .baseRent(new BigDecimal("1000.00"))
                .status(RoomStatus.VACANT)
                .build();
    }

    @Test
    void testValidTransitions() {
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        // VACANT -> OCCUPIED
        Room updated = roomService.transition(room, RoomStatus.OCCUPIED);
        assertEquals(RoomStatus.OCCUPIED, updated.getStatus());

        // OCCUPIED -> VACANT
        updated = roomService.transition(room, RoomStatus.VACANT);
        assertEquals(RoomStatus.VACANT, updated.getStatus());

        // VACANT -> MAINTENANCE
        updated = roomService.transition(room, RoomStatus.MAINTENANCE);
        assertEquals(RoomStatus.MAINTENANCE, updated.getStatus());

        // MAINTENANCE -> VACANT
        updated = roomService.transition(room, RoomStatus.VACANT);
        assertEquals(RoomStatus.VACANT, updated.getStatus());
    }

    @Test
    void testInvalidTransitionOccupiedToMaintenanceFails() {
        room.setStatus(RoomStatus.OCCUPIED);
        assertThrows(IllegalStateException.class, () -> roomService.transition(room, RoomStatus.MAINTENANCE));
    }

    @Test
    void testDeleteRoomSoftDeletes() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(contractRepository.findByRoomAndDeletedFalse(room)).thenReturn(Collections.emptyList());
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        roomService.deleteRoom(roomId);

        assertTrue(room.isDeleted());
        assertNotNull(room.getDeletedAt());
        verify(roomRepository).save(room);
        verify(roomRepository, never()).delete(any());
    }

    @Test
    void testDeleteRoomFailsWithActiveContract() {
        UUID roomId = UUID.randomUUID();
        Contract activeContract = Contract.builder().status(ContractStatus.ACTIVE).build();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(contractRepository.findByRoomAndDeletedFalse(room)).thenReturn(List.of(activeContract));

        assertThrows(IllegalStateException.class, () -> roomService.deleteRoom(roomId));
        verify(roomRepository, never()).save(any());
        verify(roomRepository, never()).delete(any());
    }
}
