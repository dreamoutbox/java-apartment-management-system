package com.dreamoutbox.ams.maintenance;

import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, UUID> {
    List<MaintenanceTicket> findByReportedByOrderByCreatedAtDesc(Tenant reportedBy);
    List<MaintenanceTicket> findByAssignedTechnicianOrderByCreatedAtDesc(User technician);
    List<MaintenanceTicket> findByStatus(TicketStatus status);
}
