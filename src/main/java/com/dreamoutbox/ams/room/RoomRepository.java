package com.dreamoutbox.ams.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findByStatus(RoomStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Room r ORDER BY CASE WHEN r.status = com.dreamoutbox.ams.room.RoomStatus.OCCUPIED THEN 0 ELSE 1 END, r.roomNumber ASC")
    List<Room> findAllSortedByOccupiedAndRoomNumber();
}
