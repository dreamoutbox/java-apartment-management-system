package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.finance.InvoiceService;
import com.dreamoutbox.ams.finance.MeterReading;
import com.dreamoutbox.ams.room.Room;
import com.dreamoutbox.ams.room.RoomService;
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
import java.util.UUID;

@Controller
@RequestMapping("/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final InvoiceService invoiceService;
    private final RoomService roomService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("readings", invoiceService.getAllMeterReadings());
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("activeTab", "meter-readings");
        return "management/meter-readings";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("activeTab", "meter-readings");
        return "management/form-meter-reading";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        MeterReading reading = invoiceService.getMeterReadingById(id);
        model.addAttribute("reading", reading);
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("activeTab", "meter-readings");
        return "management/form-meter-reading";
    }

    @PostMapping("/save")
    public String saveMeterReading(
            @RequestParam UUID roomId,
            @RequestParam String period,
            @RequestParam BigDecimal electricityReading,
            @RequestParam BigDecimal waterReading,
            RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getById(roomId);
            invoiceService.recordMeterReading(room, period, electricityReading, waterReading);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Meter reading recorded successfully for Room " + room.getRoomNumber() + " (" + period + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/meter-readings";
    }

    @PostMapping("/{id}/delete")
    public String deleteMeterReading(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteMeterReading(id);
            redirectAttributes.addFlashAttribute("successMessage", "Meter reading deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/meter-readings";
    }
}
