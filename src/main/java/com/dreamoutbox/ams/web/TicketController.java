package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.maintenance.MaintenanceTicket;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.tenant.Tenant;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.contract.ContractService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final MaintenanceService maintenanceService;
    private final UserService userService;
    private final RoomService roomService;
    private final TenantService tenantService;
    private final ContractService contractService;

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null
                && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"))) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getByUserId(currentUser.getId());
            model.addAttribute("tickets", maintenanceService.getTenantTickets(tenant));
        } else {
            model.addAttribute("tickets", maintenanceService.getAllTickets());
            model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("tenants", tenantService.getAllTenants());
        }
        model.addAttribute("activeTab", "tickets");
        return "management/tickets";
    }

    @GetMapping("/new")
    public String newForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null
                && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"))) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getByUserId(currentUser.getId());
            List<Contract> activeContracts = contractService.getTenantActiveContracts(tenant);
            List<Room> tenantRooms = activeContracts.stream()
                    .filter(c -> c.getRoom() != null)
                    .map(c -> c.getRoom())
                    .collect(Collectors.toList());

            model.addAttribute("rooms", tenantRooms);
            model.addAttribute("tenants", List.of(tenant));
        } else {
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("tenants", tenantService.getAllTenants());
            model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
        }
        model.addAttribute("activeTab", "tickets");
        return "management/form-ticket";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("ticket", maintenanceService.getById(id));
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
        model.addAttribute("activeTab", "tickets");
        return "management/form-ticket";
    }

    @PostMapping({ "", "/create" })
    public String createTicket(
            @RequestParam UUID roomId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID effectiveTenantId = tenantId;
        if (userDetails != null
                && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"))) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getByUserId(currentUser.getId());
            effectiveTenantId = tenant.getId();

            List<Contract> activeContracts = contractService.getTenantActiveContracts(tenant);
            Set<UUID> allowedRoomIds = activeContracts.stream()
                    .filter(c -> c.getRoom() != null)
                    .map(c -> c.getRoom().getId())
                    .collect(Collectors.toSet());

            if (!allowedRoomIds.contains(roomId)) {
                throw new IllegalStateException(
                        "Access Denied: You can only submit maintenance tickets for your own active leased room.");
            }
        }

        Room room = roomService.getById(roomId);
        Tenant tenant = tenantService.getById(effectiveTenantId);
        maintenanceService.createTicket(room, tenant, description, image);
        return "redirect:/tickets";
    }

    @GetMapping("/{id}")
    public String ticketDetail(@PathVariable UUID id, Model model) {
        MaintenanceTicket ticket = maintenanceService.getById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
        model.addAttribute("activeTab", "tickets");
        return "management/ticket-detail";
    }

    @PostMapping("/assign")
    public String assignTechnician(
            @RequestParam UUID ticketId,
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(required = false) com.dreamoutbox.ams.maintenance.TicketStatus status,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) MultipartFile proofImage,
            @AuthenticationPrincipal UserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            if (technicianId != null) {
                maintenanceService.assignTechnician(ticketId, technicianId, currentUser);
            }
            if (status != null) {
                maintenanceService.updateTicketStatus(ticketId, status, note != null ? note : "", proofImage,
                        currentUser);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Ticket updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/tickets/" + ticketId;
    }
}
