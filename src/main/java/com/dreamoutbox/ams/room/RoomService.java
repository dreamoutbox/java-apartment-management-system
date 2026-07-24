package com.dreamoutbox.ams.room;

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
                throw new IllegalStateException("Cannot set occupied room directly to maintenance without terminating lease");
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
        return roomRepository.findAll();
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
