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

    @PostMapping
    public String createRoom(
            @RequestParam String roomNumber,
            @RequestParam String floor,
            @RequestParam BigDecimal baseRent) {
        roomService.createRoom(roomNumber, floor, baseRent);
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/update")
    public String updateRoom(
            @PathVariable UUID id,
            @RequestParam String roomNumber,
            @RequestParam String floor,
            @RequestParam BigDecimal baseRent,
            @RequestParam RoomStatus status) {
        roomService.updateRoom(id, roomNumber, floor, baseRent, status);
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String deleteRoom(@PathVariable UUID id) {
        roomService.deleteRoom(id);
        return "redirect:/rooms";
    }
}
