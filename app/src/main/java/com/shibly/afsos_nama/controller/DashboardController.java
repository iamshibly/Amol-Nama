package com.shibly.afsos_nama.controller;

import com.shibly.afsos_nama.model.User;
import com.shibly.afsos_nama.service.DashboardService;
import com.shibly.afsos_nama.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String semester, Authentication authentication, Model model) {
        User user = userService.getUserByEmail(authentication.getName());
        var view = dashboardService.semesterView(user, semester);
        model.addAttribute("dashboard", view.data());
        model.addAttribute("semesterView", view);
        return "dashboard";
    }
}
