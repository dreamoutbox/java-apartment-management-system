package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.contract.ContractStatus;
import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final RoomService roomService;
    private final TenantService tenantService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("contracts", contractService.getAllContracts());
        model.addAttribute("activeContracts", contractService.getAllActiveContracts());
        model.addAttribute("expiringContracts", contractService.getExpiringContracts());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "contracts");
        return "management/contracts";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "contracts");
        return "management/form-contract";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("contract", contractService.getById(id));
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "contracts");
        return "management/form-contract";
    }

    @PostMapping
    public String createContract(
            @RequestParam UUID roomId,
            @RequestParam UUID tenantId,
            @RequestParam BigDecimal monthlyRent,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        contractService.createContract(roomId, tenantId, monthlyRent, LocalDate.parse(startDate), LocalDate.parse(endDate));
        return "redirect:/contracts";
    }

    @PostMapping("/{id}/update")
    public String updateContract(
            @PathVariable UUID id,
            @RequestParam BigDecimal monthlyRent,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam ContractStatus status) {
        contractService.updateContract(id, monthlyRent, LocalDate.parse(startDate), LocalDate.parse(endDate), status);
        return "redirect:/contracts";
    }

    @PostMapping("/{id}/delete")
    public String deleteContract(@PathVariable UUID id) {
        contractService.deleteContract(id);
        return "redirect:/contracts";
    }
}
