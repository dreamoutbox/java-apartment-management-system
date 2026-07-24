package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.maintenance.MaintenanceService;
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

import java.util.UUID;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final MaintenanceService maintenanceService;
    private final UserService userService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("tickets", maintenanceService.getAllTickets());
        model.addAttribute("technicians", userService.getUsersByRole(Role.TECHNICIAN));
        model.addAttribute("activeTab", "tickets");
        return "management/tickets";
    }

    @PostMapping("/assign")
    public String assignTechnician(
            @RequestParam UUID ticketId,
            @RequestParam UUID technicianId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        maintenanceService.assignTechnician(ticketId, technicianId, currentUser);
        return "redirect:/tickets";
    }
}
