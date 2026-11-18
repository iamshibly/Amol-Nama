package com.shibly.afsos_nama.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final JdbcTemplate jdbc;

    @GetMapping("/healthz")
    @ResponseBody
    public String health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return "ok";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
