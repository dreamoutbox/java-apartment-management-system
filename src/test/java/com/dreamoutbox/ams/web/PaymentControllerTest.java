package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.finance.PaymentPdfService;
import com.dreamoutbox.ams.finance.PaymentService;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentPdfService paymentPdfService;

    @Mock
    private TenantService tenantService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService, paymentPdfService, tenantService, userService);
    }

    @Test
    void testNewPaymentFormWithoutQueryParam() {
        when(tenantService.getAllTenants()).thenReturn(List.of());

        String view = paymentController.newPaymentForm(null, null, model);

        assertEquals("management/form-payment", view);
        verify(model).addAttribute("selectedTenantId", null);
        verify(model).addAttribute("activeTab", "payments");
    }

    @Test
    void testNewPaymentFormWithTenantIdQueryParam() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.getAllTenants()).thenReturn(List.of());

        String view = paymentController.newPaymentForm(tenantId.toString(), null, model);

        assertEquals("management/form-payment", view);
        verify(model).addAttribute("selectedTenantId", tenantId);
    }

    @Test
    void testNewPaymentFormWithTenantQueryParamAlias() {
        UUID tenantId = UUID.randomUUID();
        when(tenantService.getAllTenants()).thenReturn(List.of());

        String view = paymentController.newPaymentForm(null, tenantId.toString(), model);

        assertEquals("management/form-payment", view);
        verify(model).addAttribute("selectedTenantId", tenantId);
    }

    @Test
    void testNewPaymentFormWithUsernameQueryParam() {
        UUID tenantId = UUID.randomUUID();
        User user = User.builder().username("john_tenant").build();
        Tenant tenant = Tenant.builder().user(user).fullName("John Doe").build();
        tenant.setId(tenantId);

        when(tenantService.getAllTenants()).thenReturn(List.of(tenant));

        String view = paymentController.newPaymentForm("john_tenant", null, model);

        assertEquals("management/form-payment", view);
        verify(model).addAttribute("selectedTenantId", tenantId);
    }
}
