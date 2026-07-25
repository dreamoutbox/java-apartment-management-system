package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.announcement.AnnouncementService;
import com.dreamoutbox.ams.announcement.AnnouncementType;
import com.dreamoutbox.ams.user.User;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/Announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserService userService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("announcements", announcementService.getAllAnnouncements());
        model.addAttribute("activeTab", "announcements");
        return "management/announcements";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activeTab", "announcements");
        return "management/form-announcement";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("announcement", announcementService.getById(id));
        model.addAttribute("activeTab", "announcements");
        return "management/form-announcement";
    }

    @PostMapping
    public String createAnnouncement(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam AnnouncementType type,
            @RequestParam(defaultValue = "false") boolean pinned,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getByUsername(userDetails.getUsername());
        announcementService.createAnnouncement(title, content, type, pinned, currentUser);
        return "redirect:/Announcements";
    }

    @PostMapping("/{id}/update")
    public String updateAnnouncement(
            @PathVariable UUID id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam AnnouncementType type,
            @RequestParam(defaultValue = "false") boolean pinned) {
        announcementService.updateAnnouncement(id, title, content, type, pinned);
        return "redirect:/Announcements";
    }

    @PostMapping("/{id}/delete")
    public String deleteAnnouncement(@PathVariable UUID id) {
        announcementService.deleteAnnouncement(id);
        return "redirect:/Announcements";
    }
}
