package com.dreamoutbox.ams.web;

import com.dreamoutbox.ams.tenant.TenantService;
import com.dreamoutbox.ams.user.Role;
import com.dreamoutbox.ams.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TenantService tenantService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("activeTab", "users");
        return "management/users";
    }

    @PostMapping
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam Role role) {
        userService.createUser(username, password, fullName, role);
        return "redirect:/users";
    }

    @PostMapping("/{id}/update")
    public String updateUser(
            @PathVariable UUID id,
            @RequestParam String fullName,
            @RequestParam Role role,
            @RequestParam(required = false, defaultValue = "true") Boolean enabled,
            @RequestParam(required = false) String password) {
        userService.updateUser(id, fullName, role, enabled, password);
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return "redirect:/users";
    }
}
