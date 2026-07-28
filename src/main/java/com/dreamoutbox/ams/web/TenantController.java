package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final UserService userService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final MaintenanceService maintenanceService;

    @GetMapping({ "/dashboard", "" })
    public String dashboard() {
        return "redirect:/contracts";
    }

    @PostMapping("/tickets")
    public String createTicket(
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        Tenant tenant = tenantService.getByUserId(currentUser.getId());
        List<Contract> activeContracts = contractService.getTenantActiveContracts(tenant);
        if (activeContracts.isEmpty()) {
            throw new IllegalStateException("Tenant has no active contract to submit maintenance ticket");
        }

        Room room = activeContracts.get(0).getRoom();
        maintenanceService.createTicket(room, tenant, description, image);
        return "redirect:/tenant/dashboard";
    }
}
