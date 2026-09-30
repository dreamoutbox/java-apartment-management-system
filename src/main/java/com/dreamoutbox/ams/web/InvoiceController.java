package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.Invoice;
import com.dreamoutbox.ams.finance.InvoicePdfService;
import com.dreamoutbox.ams.finance.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final ContractService contractService;
    private final UserService userService;
    private final TenantService tenantService;

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"))) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getByUserId(currentUser.getId());
            model.addAttribute("invoices", invoiceService.getTenantInvoices(tenant));
        } else {
            model.addAttribute("invoices", invoiceService.getAllInvoices());
            model.addAttribute("totalOutstanding", invoiceService.getTotalOutstandingBalance());
        }
        model.addAttribute("activeTab", "invoices");
        return "management/invoices";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("contracts", contractService.getAllActiveContracts());
        model.addAttribute("activeTab", "invoices");
        return "management/form-invoice";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Invoice invoice = invoiceService.getById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("contracts", contractService.getAllActiveContracts());
        if (invoice.getContract() != null && invoice.getContract().getRoom() != null) {
            model.addAttribute("meterReading", invoiceService.getMeterReading(invoice.getContract().getRoom(), invoice.getPeriod()));
        }
        model.addAttribute("utilityRate", invoiceService.getUtilityRate(invoice.getPeriod()));
        model.addAttribute("activeTab", "invoices");
        return "management/form-invoice";
    }

    @GetMapping("/{id}")
    public String invoiceDetail(@PathVariable UUID id, Model model) {
        Invoice invoice = invoiceService.getById(id);
        model.addAttribute("invoice", invoice);

        if (invoice.getContract() != null && invoice.getContract().getRoom() != null) {
            com.dreamoutbox.ams.room.Room room = invoice.getContract().getRoom();
            String period = invoice.getPeriod();
            com.dreamoutbox.ams.finance.MeterReading currentReading = invoiceService.getMeterReading(room, period);
            com.dreamoutbox.ams.finance.UtilityRate utilityRate = invoiceService.getUtilityRate(period);

            BigDecimal elecRate = (utilityRate != null && utilityRate.getElectricityUnitPrice() != null)
                    ? utilityRate.getElectricityUnitPrice()
                    : new BigDecimal("0.25");
            BigDecimal waterRate = (utilityRate != null && utilityRate.getWaterUnitPrice() != null)
                    ? utilityRate.getWaterUnitPrice()
                    : new BigDecimal("1.50");

            BigDecimal elecUsage = (elecRate.compareTo(BigDecimal.ZERO) > 0)
                    ? invoice.getElectricityAmount().divide(elecRate, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal waterUsage = (waterRate.compareTo(BigDecimal.ZERO) > 0)
                    ? invoice.getWaterAmount().divide(waterRate, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            model.addAttribute("meterReading", currentReading);
            model.addAttribute("utilityRate", utilityRate);
            model.addAttribute("electricityRate", elecRate);
            model.addAttribute("waterRate", waterRate);
            model.addAttribute("electricityUsage", elecUsage);
            model.addAttribute("waterUsage", waterUsage);
        }

        model.addAttribute("activeTab", "invoices");
        return "management/invoice-detail";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        Invoice invoice = invoiceService.getById(id);

        BigDecimal elecRate = BigDecimal.ZERO, waterRate = BigDecimal.ZERO;
        BigDecimal elecUsage = BigDecimal.ZERO, waterUsage = BigDecimal.ZERO;

        if (invoice.getContract() != null && invoice.getContract().getRoom() != null) {
            var utilityRate = invoiceService.getUtilityRate(invoice.getPeriod());
            elecRate  = (utilityRate != null && utilityRate.getElectricityUnitPrice() != null)
                    ? utilityRate.getElectricityUnitPrice() : new BigDecimal("0.25");
            waterRate = (utilityRate != null && utilityRate.getWaterUnitPrice() != null)
                    ? utilityRate.getWaterUnitPrice() : new BigDecimal("1.50");
            elecUsage  = elecRate.compareTo(BigDecimal.ZERO)  > 0
                    ? invoice.getElectricityAmount().divide(elecRate,  2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
            waterUsage = waterRate.compareTo(BigDecimal.ZERO) > 0
                    ? invoice.getWaterAmount().divide(waterRate, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }

        byte[] pdf = invoicePdfService.generate(invoice, elecUsage, waterUsage, elecRate, waterRate);
        String filename = "invoice-" + invoice.getInvoiceNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/save")
    public String saveInvoice(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam UUID contractId,
            @RequestParam String period,
            @RequestParam(required = false) BigDecimal electricityReading,
            @RequestParam(required = false) BigDecimal waterReading,
            @RequestParam(required = false) BigDecimal electricityRate,
            @RequestParam(required = false) BigDecimal waterRate,
            @RequestParam(required = false) LocalDate dueDate,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.createOrUpdateInvoice(
                    invoiceId, contractId, period, electricityReading, waterReading, electricityRate, waterRate, dueDate);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invoice " + (invoiceId != null ? "updated" : "created") + " successfully for period " + invoice.getPeriod());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }

    @PostMapping("/generate")
    public String generateMonthlyInvoices(@RequestParam String period, RedirectAttributes redirectAttributes) {
        try {
            var generated = invoiceService.generateMonthlyInvoices(period);
            redirectAttributes.addFlashAttribute("successMessage", "Generated " + generated.size() + " invoice(s) for period " + period);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }

    @PostMapping("/utility-rates")
    public String setUtilityRates(
            @RequestParam String period,
            @RequestParam BigDecimal electricityRate,
            @RequestParam BigDecimal waterRate,
            RedirectAttributes redirectAttributes) {
        try {
            invoiceService.setUtilityRate(period, electricityRate, waterRate);
            redirectAttributes.addFlashAttribute("successMessage", "Utility rates saved for period " + period);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }

    @PostMapping("/{id}/delete")
    public String deleteInvoice(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }
}
