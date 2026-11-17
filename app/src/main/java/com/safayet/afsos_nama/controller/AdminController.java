package com.safayet.afsos_nama.controller;

import com.safayet.afsos_nama.model.Afsos;
import com.safayet.afsos_nama.service.AfsosService;
import com.safayet.afsos_nama.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final AfsosService afsosService;

    @GetMapping
    public String dashboard(Model model) {
        List<Afsos> pendingConfessions = afsosService.getPendingConfessions();

        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingConfessions", pendingConfessions);
        model.addAttribute("publishedConfessions", afsosService.getApprovedConfessions());
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("totalAfsos", afsosService.countAllAfsos());
        model.addAttribute("pendingCount", pendingConfessions.size());

        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean updated = userService.toggleUser(id, authentication.getName());
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "User status updated." : "This account cannot be changed.");
        return "redirect:/admin";
    }

    @PostMapping("/confessions/{id}/approve")
    public String approve(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean updated = afsosService.moderateConfession(id, userService.getUserByEmail(authentication.getName()), true);
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "Confession approved." : "This confession is no longer awaiting approval.");
        return "redirect:/admin";
    }

    @PostMapping("/confessions/{id}/hide")
    public String hide(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean updated = afsosService.moderateConfession(id, userService.getUserByEmail(authentication.getName()), false);
        redirectAttributes.addFlashAttribute(updated ? "successMessage" : "errorMessage",
                updated ? "Confession removed." : "This confession is no longer shared.");
        return "redirect:/admin";
    }
}
