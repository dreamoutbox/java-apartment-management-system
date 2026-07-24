package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.maintenance.TicketStatus;
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

import java.util.UUID;

@Controller
@RequestMapping("/technician")
@RequiredArgsConstructor
public class TechnicianController {

    private final MaintenanceService maintenanceService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        model.addAttribute("tickets", maintenanceService.getTechnicianTickets(currentUser));
        return "technician/dashboard";
    }

    @PostMapping("/tickets/update")
    public String updateTicket(
            @RequestParam UUID ticketId,
            @RequestParam TicketStatus status,
            @RequestParam String note,
            @RequestParam(required = false) MultipartFile proofImage,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        maintenanceService.updateTicketStatus(ticketId, status, note, proofImage, currentUser);
        return "redirect:/technician/dashboard";
    }
}
