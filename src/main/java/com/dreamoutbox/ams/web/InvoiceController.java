package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.ContractService;
import com.dreamoutbox.ams.finance.Invoice;
import com.dreamoutbox.ams.finance.InvoiceService;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ContractService contractService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        model.addAttribute("totalOutstanding", invoiceService.getTotalOutstandingBalance());
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
        model.addAttribute("activeTab", "invoices");
        return "management/invoice-detail";
    }

    @PostMapping("/save")
    public String saveInvoice(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam UUID contractId,
            @RequestParam String period,
            @RequestParam BigDecimal electricityReading,
            @RequestParam BigDecimal waterReading,
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
}
