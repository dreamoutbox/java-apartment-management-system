package com.dreamoutbox.ams.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

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
}
