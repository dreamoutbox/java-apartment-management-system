package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.finance.Invoice;
import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.finance.InvoiceStatus;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class InvoiceControllerViewTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testInvoiceDetailRendersWithCreatePaymentButton() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        User user = User.builder().username("tenant@test.com").build();
        Tenant tenant = Tenant.builder().fullName("Test Tenant").user(user).build();
        tenant.setId(tenantId);

        Room room = Room.builder().roomNumber("201").floor("2").build();
        Contract contract = Contract.builder().tenant(tenant).room(room).build();

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .period("2026-07")
                .invoiceNumber("INV-202607-00001")
                .rentAmount(new BigDecimal("1000.00"))
                .electricityAmount(new BigDecimal("50.00"))
                .waterAmount(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("1070.00"))
                .balanceRemaining(new BigDecimal("1070.00"))
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDate.of(2026, 7, 5))
                .build();
        invoice.setId(invoiceId);

        when(invoiceService.getById(invoiceId)).thenReturn(invoice);

        mockMvc.perform(get("/invoices/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Payment")))
                .andExpect(content().string(containsString("/payments/new?tenantId=" + tenantId)));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testInvoiceDetailRendersWhenContractIsNull() throws Exception {
        UUID invoiceId = UUID.randomUUID();

        Invoice invoice = Invoice.builder()
                .contract(null)
                .period("2026-07")
                .invoiceNumber("INV-202607-00002")
                .rentAmount(new BigDecimal("1000.00"))
                .electricityAmount(BigDecimal.ZERO)
                .waterAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("1000.00"))
                .balanceRemaining(new BigDecimal("1000.00"))
                .status(InvoiceStatus.UNPAID)
                .dueDate(LocalDate.of(2026, 7, 5))
                .build();
        invoice.setId(invoiceId);

        when(invoiceService.getById(invoiceId)).thenReturn(invoice);

        mockMvc.perform(get("/invoices/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create Payment")))
                .andExpect(content().string(containsString("/payments/new")));
    }
}
