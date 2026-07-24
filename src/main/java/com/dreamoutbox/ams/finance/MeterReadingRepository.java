package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.room.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    Optional<MeterReading> findByRoomAndPeriod(Room room, String period);
    List<MeterReading> findByRoom(Room room);
}
