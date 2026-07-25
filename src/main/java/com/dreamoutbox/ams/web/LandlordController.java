package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.announcement.AnnouncementService;
import com.dreamoutbox.ams.announcement.AnnouncementType;
import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.maintenance.TicketStatus;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/landlord")
@RequiredArgsConstructor
public class LandlordController {

    private final UserService userService;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final MaintenanceService maintenanceService;
    private final AnnouncementService announcementService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeContracts", contractService.getAllActiveContracts());
        model.addAttribute("expiringContracts", contractService.getExpiringContracts());
        model.addAttribute("totalOutstandingBalance", invoiceService.getTotalOutstandingBalance());
        model.addAttribute("tickets", maintenanceService.getAllTickets());
        model.addAttribute("announcements", announcementService.getAllAnnouncements());
        model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
        return "dashboard";
    }

    @PostMapping("/users")
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam Role role) {
        userService.createUser(username, password, fullName, role);
        return "redirect:/landlord/dashboard";
    }

    @PostMapping("/rooms")
    public String createRoom(
            @RequestParam String roomNumber,
            @RequestParam String floor,
            @RequestParam BigDecimal baseRent,
            RedirectAttributes redirectAttributes) {
        try {
            roomService.createRoom(roomNumber, floor, baseRent);
            redirectAttributes.addFlashAttribute("successMessage", "Room created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/landlord/dashboard";
    }

    @PostMapping("/contracts")
    public String createContract(
            @RequestParam UUID roomId,
            @RequestParam UUID tenantId,
            @RequestParam BigDecimal monthlyRent,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        contractService.createContract(roomId, tenantId, monthlyRent, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return "redirect:/landlord/dashboard";
    }

    @PostMapping("/announcements")
    public String createAnnouncement(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam AnnouncementType type,
            @RequestParam(defaultValue = "false") boolean pinned,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        announcementService.createAnnouncement(title, content, type, pinned, currentUser);
        return "redirect:/landlord/dashboard";
    }

    @PostMapping("/tickets/assign")
    public String assignTechnician(
            @RequestParam UUID ticketId,
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) MultipartFile proofImage,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            if (technicianId != null) {
                maintenanceService.assignTechnician(ticketId, technicianId, currentUser);
            }
            if (status != null) {
                maintenanceService.updateTicketStatus(ticketId, status, note != null ? note : "", proofImage, currentUser);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Ticket updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/landlord/dashboard";
    }
}
