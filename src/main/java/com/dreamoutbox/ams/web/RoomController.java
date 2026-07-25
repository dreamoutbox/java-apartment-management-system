package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.room.RoomService;
import com.dreamoutbox.ams.room.RoomStatus;
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
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("activeTab", "rooms");
        return "management/rooms";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activeTab", "rooms");
        return "management/form-room";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("room", roomService.getById(id));
        model.addAttribute("activeTab", "rooms");
        return "management/form-room";
    }

    @PostMapping
    public String createRoom(
            @RequestParam String roomNumber,
            @RequestParam String floor,
            @RequestParam BigDecimal baseRent,
            RedirectAttributes redirectAttributes) {
        try {
            roomService.createRoom(roomNumber, floor, baseRent);
            redirectAttributes.addFlashAttribute("successMessage", "Room created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/update")
    public String updateRoom(
            @PathVariable UUID id,
            @RequestParam String roomNumber,
            @RequestParam String floor,
            @RequestParam BigDecimal baseRent,
            @RequestParam RoomStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            roomService.updateRoom(id, roomNumber, floor, baseRent, status);
            redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String deleteRoom(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cannot delete room: it may have active contracts, invoices, or maintenance tickets.");
        }
        return "redirect:/rooms";
    }
}
