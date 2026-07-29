package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.finance.Payment;
import com.dreamoutbox.ams.finance.PaymentPdfService;
import com.dreamoutbox.ams.finance.PaymentService;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentPdfService paymentPdfService;
    private final TenantService tenantService;
    private final UserService userService;

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null
                && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TENANT"))) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getByUserId(currentUser.getId());
            model.addAttribute("payments", paymentService.getPaymentsByTenant(tenant));
        } else {
            model.addAttribute("payments", paymentService.getAllPayments());
            model.addAttribute("tenants", tenantService.getAllTenants());
        }
        model.addAttribute("activeTab", "payments");
        return "management/payments";
    }

    @GetMapping("/new")
    public String newPaymentForm(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "payments");
        return "management/form-payment";
    }

    @GetMapping("/{id}")
    public String paymentDetail(@PathVariable UUID id, Model model) {
        Payment payment = paymentService.getById(id);
        model.addAttribute("payment", payment);
        model.addAttribute("activeTab", "payments");
        return "management/payment-detail";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable UUID id) {
        Payment payment = paymentService.getById(id);
        byte[] pdf = paymentPdfService.generate(payment);
        String filename = "receipt-" + payment.getId() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/create")
    public String recordPayment(
            @RequestParam UUID tenantId,
            @RequestParam BigDecimal amount,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            Tenant tenant = tenantService.getById(tenantId);
            Payment payment = paymentService.recordPayment(tenant, amount, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment recorded successfully. Receipt #" + payment.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payments";
    }
}
