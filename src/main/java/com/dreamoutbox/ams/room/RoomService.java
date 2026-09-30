package com.dreamoutbox.ams.room;

import com.dreamoutbox.ams.contract.ContractRepository;
import com.dreamoutbox.ams.contract.ContractStatus;
import com.dreamoutbox.ams.finance.InvoiceRepository;
import com.dreamoutbox.ams.finance.MeterReadingRepository;
import com.dreamoutbox.ams.maintenance.MaintenanceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final MaintenanceTicketRepository ticketRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Room createRoom(String roomNumber, String floor, BigDecimal baseRent) {
        if (roomRepository.findByRoomNumber(roomNumber).isPresent()) {
            throw new IllegalArgumentException("Room number already exists: " + roomNumber);
        }
        Room room = Room.builder()
                .roomNumber(roomNumber)
                .floor(floor)
                .baseRent(baseRent)
                .status(RoomStatus.VACANT)
                .build();
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(UUID id, String roomNumber, String floor, BigDecimal baseRent, RoomStatus status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));

        room.setRoomNumber(roomNumber);
        room.setFloor(floor);
        room.setBaseRent(baseRent);
        if (status != null && room.getStatus() != status) {
            transition(room, status);
        }
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));

        var contracts = contractRepository.findByRoomAndDeletedFalse(room);
        boolean hasActiveContract = room.getStatus() == RoomStatus.OCCUPIED ||
                contracts.stream().anyMatch(c -> c.getStatus() == ContractStatus.ACTIVE);

        if (hasActiveContract) {
            throw new IllegalStateException(
                    "Cannot delete room while it has an active lease contract. Please terminate or expire the contract first.");
        }

        // 1. Delete associated maintenance tickets (cascades to ticket images &
        // history)
        var tickets = ticketRepository.findByRoom(room);
        ticketRepository.deleteAll(tickets);

        // 2. Delete associated meter readings
        var readings = meterReadingRepository.findByRoom(room);
        meterReadingRepository.deleteAll(readings);

        // 3. Delete inactive/historical contracts and their invoices
        for (var contract : contracts) {
            var invoices = invoiceRepository.findByContract(contract);
            invoiceRepository.deleteAll(invoices);
        }
        contractRepository.deleteAll(contracts);

        // 4. Delete room
        roomRepository.delete(room);
    }

    @Transactional
    public Room transition(UUID roomId, RoomStatus targetStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        return transition(room, targetStatus);
    }

    @Transactional
    public Room transition(Room room, RoomStatus targetStatus) {
        RoomStatus current = room.getStatus();
        if (current == targetStatus) {
            return room;
        }

        if (current == RoomStatus.OCCUPIED && targetStatus == RoomStatus.VACANT) {
            // End contract transition allowed
        } else if (current == RoomStatus.VACANT && targetStatus == RoomStatus.OCCUPIED) {
            // New active contract transition allowed
        } else if (targetStatus == RoomStatus.MAINTENANCE) {
            if (current == RoomStatus.OCCUPIED) {
                throw new IllegalStateException(
                        "Cannot set occupied room directly to maintenance without terminating lease");
            }
        } else if (current == RoomStatus.MAINTENANCE && targetStatus == RoomStatus.VACANT) {
            // Maintenance completed transition allowed
        } else {
            throw new IllegalStateException("Invalid status transition from " + current + " to " + targetStatus);
        }

        room.setStatus(targetStatus);
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        List<Room> rooms = roomRepository.findAllSortedByOccupiedAndRoomNumber();
        rooms.sort((r1, r2) -> {
            boolean r1Occupied = r1.getStatus() == RoomStatus.OCCUPIED;
            boolean r2Occupied = r2.getStatus() == RoomStatus.OCCUPIED;
            if (r1Occupied != r2Occupied) {
                return r1Occupied ? -1 : 1;
            }
            try {
                int n1 = Integer.parseInt(r1.getRoomNumber());
                int n2 = Integer.parseInt(r2.getRoomNumber());
                return Integer.compare(n1, n2);
            } catch (NumberFormatException e) {
                return (r1.getRoomNumber() != null && r2.getRoomNumber() != null)
                        ? r1.getRoomNumber().compareTo(r2.getRoomNumber())
                        : 0;
            }
        });
        return rooms;
    }

    @Transactional(readOnly = true)
    public List<Room> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Room getById(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + id));
    }
}
