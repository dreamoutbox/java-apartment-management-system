package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceTest {

    @Test
    void testSafeGettersWithValidContract() {
        User user = User.builder().username("tenant1@example.com").build();
        Tenant tenant = Tenant.builder()
                .fullName("System Tenant 1")
                .phone("0812345671")
                .user(user)
                .build();
        Room room = Room.builder()
                .roomNumber("101")
                .floor("1")
                .build();
        Contract contract = Contract.builder()
                .tenant(tenant)
                .room(room)
                .build();

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .period("2026-01")
                .rentAmount(new BigDecimal("1000"))
                .electricityAmount(new BigDecimal("10"))
                .waterAmount(new BigDecimal("10"))
                .totalAmount(new BigDecimal("1020"))
                .balanceRemaining(BigDecimal.ZERO)
                .build();

        assertEquals("System Tenant 1", invoice.getTenantName());
        assertEquals("101", invoice.getRoomNumber());
        assertEquals("0812345671", invoice.getTenantPhone());
        assertEquals("tenant1@example.com", invoice.getTenantEmail());
        assertEquals("1", invoice.getFloor());
    }

    @Test
    void testSafeGettersWithNullContract() {
        Invoice invoice = Invoice.builder()
                .contract(null)
                .period("2026-01")
                .build();

        assertEquals("N/A", invoice.getTenantName());
        assertEquals("N/A", invoice.getRoomNumber());
        assertEquals("N/A", invoice.getTenantPhone());
        assertEquals("N/A", invoice.getTenantEmail());
        assertEquals("N/A", invoice.getFloor());
    }

    @Test
    void testSafeGettersWhenContractThrowsEntityNotFound() {
        Contract brokenProxy = mock(Contract.class);
        when(brokenProxy.getTenant()).thenThrow(new EntityNotFoundException("Entity not found"));
        when(brokenProxy.getRoom()).thenThrow(new EntityNotFoundException("Entity not found"));

        Invoice invoice = Invoice.builder()
                .contract(brokenProxy)
                .period("2026-01")
                .build();

        assertEquals("N/A", invoice.getTenantName());
        assertEquals("N/A", invoice.getRoomNumber());
        assertEquals("N/A", invoice.getTenantPhone());
        assertEquals("N/A", invoice.getTenantEmail());
        assertEquals("N/A", invoice.getFloor());
    }
}
