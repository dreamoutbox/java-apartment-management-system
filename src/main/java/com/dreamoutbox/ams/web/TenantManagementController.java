package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.finance.PaymentService;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tenants")
@RequiredArgsConstructor
public class TenantManagementController {

    private final TenantService tenantService;
    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "tenants");
        return "management/tenants";
    }

    @GetMapping("/{id}")
    public String tenantDetail(@PathVariable UUID id, Model model) {
        Tenant tenant = tenantService.getById(id);
        var contracts = contractService.getTenantActiveContracts(tenant);
        var invoices = invoiceService.getTenantInvoices(tenant);
        var payments = paymentService.getPaymentsByTenant(tenant);

        var roomIds = contracts.stream()
                .filter(c -> c.getRoom() != null)
                .map(c -> c.getRoom().getId())
                .collect(Collectors.toSet());
        var readings = invoiceService.getAllMeterReadings().stream()
                .filter(r -> r.getRoom() != null && roomIds.contains(r.getRoom().getId()))
                .collect(Collectors.toList());

        model.addAttribute("tenant", tenant);
        model.addAttribute("contracts", contracts);
        model.addAttribute("readings", readings);
        model.addAttribute("invoices", invoices);
        model.addAttribute("payments", payments);
        model.addAttribute("activeTab", "tenants");

        return "management/tenant-detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activeTab", "tenants");
        return "management/form-tenant";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("tenant", tenantService.getById(id));
        model.addAttribute("activeTab", "tenants");
        return "management/form-tenant";
    }

    @PostMapping
    public String createTenant(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String idCardNumber,
            @RequestParam(required = false) MultipartFile idCardImage) {
        tenantService.createTenantWithAccount(username, password, fullName, phone, email, idCardNumber, idCardImage);
        return "redirect:/tenants";
    }

    @PostMapping("/{id}/update")
    public String updateTenant(
            @PathVariable UUID id,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String idCardNumber,
            @RequestParam(required = false) MultipartFile idCardImage) {
        tenantService.updateTenant(id, fullName, phone, email, idCardNumber, idCardImage);
        return "redirect:/tenants";
    }

    @PostMapping("/{id}/delete")
    public String deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return "redirect:/tenants";
    }
}
