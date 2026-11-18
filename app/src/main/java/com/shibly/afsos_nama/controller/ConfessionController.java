package com.shibly.afsos_nama.controller;

import com.shibly.afsos_nama.model.enums.ReactionType;
import com.shibly.afsos_nama.service.AfsosService;
import com.shibly.afsos_nama.service.ReactionService;
import com.shibly.afsos_nama.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/confessions")
public class ConfessionController {

    private final AfsosService afsosService;
    private final ReactionService reactionService;
    private final UserService userService;

    @GetMapping
    public String feed(Authentication authentication, Model model) {
        var confessions = afsosService.getApprovedConfessions();
        model.addAttribute("confessions", confessions);
        model.addAttribute("reactionTypes", ReactionType.values());
        var reactions = reactionService.summaries(confessions, userId(authentication));
        model.addAttribute("reactions", reactions);
        model.addAttribute("reactedPostIds", reactions.entrySet().stream()
                .filter(entry -> entry.getValue().values().stream().anyMatch(ReactionService.ReactionCount::selected))
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet()));
        return "confessions/feed";
    }

    @PostMapping("/{id}/react/{reactionType}")
    public String react(@PathVariable Integer id, @PathVariable ReactionType reactionType,
                        Authentication authentication) {
        reactionService.setReaction(id, userId(authentication), reactionType);
        return "redirect:/confessions";
    }

    @PostMapping("/{id}/reaction/remove")
    public String removeReaction(@PathVariable Integer id, Authentication authentication) {
        reactionService.setReaction(id, userId(authentication), null);
        return "redirect:/confessions";
    }

    private int userId(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName()).getId();
    }
}
