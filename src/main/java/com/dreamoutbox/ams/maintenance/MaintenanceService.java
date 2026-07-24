package com.dreamoutbox.ams.maintenance;

import com.dreamoutbox.ams.common.FileStorageService;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public MaintenanceTicket createTicket(Room room, Tenant reportedBy, String description, MultipartFile reportedImage) {
        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .room(room)
                .reportedBy(reportedBy)
                .description(description)
                .status(TicketStatus.PENDING)
                .build();

        if (reportedImage != null && !reportedImage.isEmpty()) {
            String path = fileStorageService.storeFile(reportedImage, "tickets");
            TicketImage image = TicketImage.builder()
                    .ticket(ticket)
                    .imagePath(path)
                    .type(TicketImageType.REPORTED)
                    .build();
            ticket.getImages().add(image);
        }

        TicketStatusUpdate initialHistory = TicketStatusUpdate.builder()
                .ticket(ticket)
                .status(TicketStatus.PENDING)
                .updatedBy(reportedBy.getUser())
                .note("Ticket submitted by tenant")
                .build();
        ticket.getHistory().add(initialHistory);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public MaintenanceTicket assignTechnician(UUID ticketId, UUID technicianUserId, User managerOrLandlord) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        User technician = userRepository.findById(technicianUserId)
                .orElseThrow(() -> new IllegalArgumentException("Technician user not found: " + technicianUserId));

        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Target user is not a technician");
        }

        ticket.setAssignedTechnician(technician);

        TicketStatusUpdate update = TicketStatusUpdate.builder()
                .ticket(ticket)
                .status(ticket.getStatus())
                .updatedBy(managerOrLandlord)
                .note("Assigned to technician: " + technician.getFullName())
                .build();
        ticket.getHistory().add(update);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public MaintenanceTicket updateTicketStatus(UUID ticketId, TicketStatus newStatus, String note, MultipartFile proofImage, User currentUser) {
        MaintenanceTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        // Security check: Technicians can only update tickets assigned to them
        if (currentUser.getRole() == Role.TECHNICIAN) {
            if (ticket.getAssignedTechnician() == null || !ticket.getAssignedTechnician().getId().equals(currentUser.getId())) {
                throw new IllegalStateException("Technician can only update tickets assigned to them");
            }
        }

        if (proofImage != null && !proofImage.isEmpty()) {
            String path = fileStorageService.storeFile(proofImage, "tickets");
            TicketImage image = TicketImage.builder()
                    .ticket(ticket)
                    .imagePath(path)
                    .type(TicketImageType.PROOF_OF_FIX)
                    .build();
            ticket.getImages().add(image);
        }

        if (newStatus == TicketStatus.COMPLETED) {
            boolean hasProofOfFix = ticket.getImages().stream()
                    .anyMatch(img -> img.getType() == TicketImageType.PROOF_OF_FIX);
            if (!hasProofOfFix) {
                throw new IllegalStateException("Proof of fix image is required before completing ticket");
            }
        }

        ticket.setStatus(newStatus);

        TicketStatusUpdate update = TicketStatusUpdate.builder()
                .ticket(ticket)
                .status(newStatus)
                .updatedBy(currentUser)
                .note(note)
                .build();
        ticket.getHistory().add(update);

        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getTenantTickets(Tenant tenant) {
        return ticketRepository.findByReportedByOrderByCreatedAtDesc(tenant);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getTechnicianTickets(User technician) {
        return ticketRepository.findByAssignedTechnicianOrderByCreatedAtDesc(technician);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public MaintenanceTicket getById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }
}
