package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.tenant.Tenant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

        Optional<Invoice> findByContractAndPeriod(Contract contract, String period);

        List<Invoice> findByContract(Contract contract);

        List<Invoice> findByContractTenantOrderByPeriodAsc(Tenant tenant);

        List<Invoice> findByContractTenantOrderByPeriodDesc(Tenant tenant);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT i FROM Invoice i WHERE i.contract.tenant = :tenant AND i.status IN :statuses ORDER BY i.period ASC")
        List<Invoice> findUnpaidInvoicesForTenantForUpdate(
                        @Param("tenant") Tenant tenant,
                        @Param("statuses") List<InvoiceStatus> statuses);

        List<Invoice> findByContractTenantAndStatusInOrderByPeriodAsc(
                        Tenant tenant, List<InvoiceStatus> statuses);

        @Query("SELECT i FROM Invoice i WHERE i.status != 'PAID' AND i.dueDate < :today")
        List<Invoice> findOverdueInvoices(@Param("today") LocalDate today);

        List<Invoice> findAllByOrderByPeriodDesc();

        List<Invoice> findAllByOrderByCreatedAtAsc();

        @Query("SELECT COALESCE(SUM(i.balanceRemaining), 0) FROM Invoice i WHERE i.status != 'PAID'")
        Double calculateTotalOutstandingBalance();
}
