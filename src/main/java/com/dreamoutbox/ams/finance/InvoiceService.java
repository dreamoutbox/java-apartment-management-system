package com.dreamoutbox.ams.finance;

import com.dreamoutbox.ams.contract.Contract;
import com.dreamoutbox.ams.contract.ContractRepository;
import com.dreamoutbox.ams.contract.ContractStatus;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.tenant.Tenant;
import com.dreamoutbox.ams.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UtilityRateRepository utilityRateRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final ContractRepository contractRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public UtilityRate setUtilityRate(String period, BigDecimal electricityRate, BigDecimal waterRate) {
        UtilityRate rate = utilityRateRepository.findByPeriod(period)
                .orElse(UtilityRate.builder().period(period).build());
        rate.setElectricityUnitPrice(electricityRate);
        rate.setWaterUnitPrice(waterRate);
        return utilityRateRepository.save(rate);
    }

    @Transactional
    public MeterReading recordMeterReading(Room room, String period, BigDecimal electricity, BigDecimal water) {
        MeterReading reading = meterReadingRepository.findByRoomAndPeriod(room, period)
                .orElse(MeterReading.builder().room(room).period(period).build());
        reading.setElectricityReading(electricity);
        reading.setWaterReading(water);
        return meterReadingRepository.save(reading);
    }

    @Transactional
    public Invoice generateInvoice(Contract contract, String period) {
        Optional<Invoice> existing = invoiceRepository.findByContractAndPeriod(contract, period);
        if (existing.isPresent()) {
            return existing.get();
        }

        UtilityRate rate = utilityRateRepository.findByPeriod(period)
                .orElseThrow(() -> new IllegalStateException("Utility rate not set for period: " + period));

        Room room = contract.getRoom();
        MeterReading currentReading = meterReadingRepository.findByRoomAndPeriod(room, period)
                .orElseThrow(() -> new IllegalStateException("Meter reading not found for room " + room.getRoomNumber() + " in period " + period));

        // Find prior period reading
        YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        String priorPeriod = ym.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Optional<MeterReading> priorReadingOpt = meterReadingRepository.findByRoomAndPeriod(room, priorPeriod);

        BigDecimal electricityUsage = currentReading.getElectricityReading();
        BigDecimal waterUsage = currentReading.getWaterReading();

        if (priorReadingOpt.isPresent()) {
            MeterReading prior = priorReadingOpt.get();
            electricityUsage = currentReading.getElectricityReading().subtract(prior.getElectricityReading()).max(BigDecimal.ZERO);
            waterUsage = currentReading.getWaterReading().subtract(prior.getWaterReading()).max(BigDecimal.ZERO);
        }

        BigDecimal electricityAmount = electricityUsage.multiply(rate.getElectricityUnitPrice());
        BigDecimal waterAmount = waterUsage.multiply(rate.getWaterUnitPrice());
        BigDecimal rentAmount = contract.getMonthlyRent();
        BigDecimal totalAmount = rentAmount.add(electricityAmount).add(waterAmount);

        LocalDate dueDate = ym.atDay(10); // Default due date 10th of the month

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .period(period)
                .rentAmount(rentAmount)
                .electricityAmount(electricityAmount)
                .waterAmount(waterAmount)
                .totalAmount(totalAmount)
                .balanceRemaining(totalAmount)
                .status(InvoiceStatus.UNPAID)
                .dueDate(dueDate)
                .build();

        // Check if tenant has credit balance to apply
        Tenant tenant = contract.getTenant();
        if (tenant.getCreditBalance() != null && tenant.getCreditBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal credit = tenant.getCreditBalance();
            BigDecimal applied = credit.min(totalAmount);
            invoice.setBalanceRemaining(totalAmount.subtract(applied));
            tenant.setCreditBalance(credit.subtract(applied));
            if (invoice.getBalanceRemaining().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            }
            tenantRepository.save(tenant);
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public List<Invoice> generateMonthlyInvoices(String period) {
        List<Contract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        return activeContracts.stream()
                .map(contract -> generateInvoice(contract, period))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Invoice> getTenantInvoices(Tenant tenant) {
        return invoiceRepository.findByContractTenantOrderByPeriodAsc(tenant);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Double getTotalOutstandingBalance() {
        return invoiceRepository.calculateTotalOutstandingBalance();
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByPeriodDesc();
    }

    @Transactional
    public Invoice createOrUpdateInvoice(
            UUID invoiceId,
            UUID contractId,
            String period,
            BigDecimal electricityReading,
            BigDecimal waterReading,
            BigDecimal electricityRate,
            BigDecimal waterRate,
            LocalDate dueDate) {

        Contract contract = (invoiceId != null)
                ? invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId))
                        .getContract()
                : contractRepository.findById(contractId)
                        .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        Room room = contract.getRoom();

        // 1. Record/Update meter reading for room and period
        MeterReading currentReading;
        if (electricityReading != null && waterReading != null) {
            currentReading = recordMeterReading(room, period, electricityReading, waterReading);
        } else {
            currentReading = meterReadingRepository.findByRoomAndPeriod(room, period)
                    .orElse(MeterReading.builder()
                            .room(room)
                            .period(period)
                            .electricityReading(BigDecimal.ZERO)
                            .waterReading(BigDecimal.ZERO)
                            .build());
        }

        // 2. Record/Update utility rates if rates are provided
        if (electricityRate != null && waterRate != null) {
            setUtilityRate(period, electricityRate, waterRate);
        }

        UtilityRate rate = utilityRateRepository.findByPeriod(period)
                .orElseGet(() -> UtilityRate.builder()
                        .period(period)
                        .electricityUnitPrice(electricityRate != null ? electricityRate : new BigDecimal("0.25"))
                        .waterUnitPrice(waterRate != null ? waterRate : new BigDecimal("1.50"))
                        .build());
        if (rate.getId() == null) {
            utilityRateRepository.save(rate);
        }

        // 3. Calculate usages
        YearMonth ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        String priorPeriod = ym.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Optional<MeterReading> priorReadingOpt = meterReadingRepository.findByRoomAndPeriod(room, priorPeriod);

        BigDecimal electricityUsage = currentReading.getElectricityReading();
        BigDecimal waterUsage = currentReading.getWaterReading();

        if (priorReadingOpt.isPresent()) {
            MeterReading prior = priorReadingOpt.get();
            electricityUsage = currentReading.getElectricityReading().subtract(prior.getElectricityReading()).max(BigDecimal.ZERO);
            waterUsage = currentReading.getWaterReading().subtract(prior.getWaterReading()).max(BigDecimal.ZERO);
        }

        BigDecimal electricityAmount = electricityUsage.multiply(rate.getElectricityUnitPrice());
        BigDecimal waterAmount = waterUsage.multiply(rate.getWaterUnitPrice());
        BigDecimal rentAmount = contract.getMonthlyRent();
        BigDecimal totalAmount = rentAmount.add(electricityAmount).add(waterAmount);

        LocalDate calcDueDate = (dueDate != null) ? dueDate : ym.atDay(10);

        Invoice invoice;
        if (invoiceId != null) {
            invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));

            BigDecimal amountPaid = invoice.getTotalAmount().subtract(invoice.getBalanceRemaining());
            invoice.setPeriod(period);
            invoice.setRentAmount(rentAmount);
            invoice.setElectricityAmount(electricityAmount);
            invoice.setWaterAmount(waterAmount);
            invoice.setTotalAmount(totalAmount);
            invoice.setBalanceRemaining(totalAmount.subtract(amountPaid).max(BigDecimal.ZERO));
            invoice.setDueDate(calcDueDate);
            if (invoice.getBalanceRemaining().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            } else {
                invoice.setStatus(InvoiceStatus.UNPAID);
            }
        } else {
            Optional<Invoice> existing = invoiceRepository.findByContractAndPeriod(contract, period);
            if (existing.isPresent()) {
                invoice = existing.get();
                invoice.setRentAmount(rentAmount);
                invoice.setElectricityAmount(electricityAmount);
                invoice.setWaterAmount(waterAmount);
                invoice.setTotalAmount(totalAmount);
                invoice.setBalanceRemaining(totalAmount);
                invoice.setDueDate(calcDueDate);
            } else {
                invoice = Invoice.builder()
                        .contract(contract)
                        .period(period)
                        .rentAmount(rentAmount)
                        .electricityAmount(electricityAmount)
                        .waterAmount(waterAmount)
                        .totalAmount(totalAmount)
                        .balanceRemaining(totalAmount)
                        .status(InvoiceStatus.UNPAID)
                        .dueDate(calcDueDate)
                        .build();
            }

            // Apply tenant credit if available
            Tenant tenant = contract.getTenant();
            if (tenant.getCreditBalance() != null && tenant.getCreditBalance().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal credit = tenant.getCreditBalance();
                BigDecimal applied = credit.min(totalAmount);
                invoice.setBalanceRemaining(totalAmount.subtract(applied));
                tenant.setCreditBalance(credit.subtract(applied));
                if (invoice.getBalanceRemaining().compareTo(BigDecimal.ZERO) == 0) {
                    invoice.setStatus(InvoiceStatus.PAID);
                } else {
                    invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
                }
                tenantRepository.save(tenant);
            }
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<MeterReading> getAllMeterReadings() {
        return meterReadingRepository.findAllByOrderByPeriodDescRoomRoomNumberAsc();
    }

    @Transactional(readOnly = true)
    public MeterReading getMeterReadingById(UUID id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meter reading not found: " + id));
    }

    @Transactional
    public void deleteMeterReading(UUID id) {
        meterReadingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public MeterReading getMeterReading(Room room, String period) {
        return meterReadingRepository.findByRoomAndPeriod(room, period).orElse(null);
    }

    @Transactional(readOnly = true)
    public UtilityRate getUtilityRate(String period) {
        return utilityRateRepository.findByPeriod(period).orElse(null);
    }

    @Transactional(readOnly = true)
    public Invoice getById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }
}
