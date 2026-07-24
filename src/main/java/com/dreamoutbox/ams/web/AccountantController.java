package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.finance.PaymentService;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
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

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/accountant")
@RequiredArgsConstructor
public class AccountantController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TenantService tenantService;
    private final ContractService contractService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeContracts", contractService.getAllActiveContracts());
        model.addAttribute("overdueInvoices", invoiceService.getOverdueInvoices());
        model.addAttribute("totalOutstanding", invoiceService.getTotalOutstandingBalance());
        return "accountant/dashboard";
    }

    @PostMapping("/utility-rates")
    public String setUtilityRates(
            @RequestParam String period,
            @RequestParam BigDecimal electricityRate,
            @RequestParam BigDecimal waterRate) {
        invoiceService.setUtilityRate(period, electricityRate, waterRate);
        return "redirect:/accountant/dashboard";
    }

    @PostMapping("/invoices/generate")
    public String generateMonthlyInvoices(@RequestParam String period) {
        invoiceService.generateMonthlyInvoices(period);
        return "redirect:/accountant/dashboard";
    }

    @PostMapping("/payments")
    public String recordPayment(
            @RequestParam UUID tenantId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        Tenant tenant = tenantService.getById(tenantId);
        paymentService.recordPayment(tenant, amount, currentUser);
        return "redirect:/accountant/dashboard";
    }
}
