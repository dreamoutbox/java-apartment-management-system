package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.maintenance.MaintenanceService;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/landlord")
@RequiredArgsConstructor
public class LandlordController {

    private final RoomService roomService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final MaintenanceService maintenanceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeContracts", contractService.getAllActiveContracts());
        model.addAttribute("expiringContracts", contractService.getExpiringContracts());
        model.addAttribute("totalOutstandingBalance", invoiceService.getTotalOutstandingBalance());
        model.addAttribute("tickets", maintenanceService.getAllTickets());
        return "landlord/dashboard";
    }
}
