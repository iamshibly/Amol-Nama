package com.safayet.afsos_nama.controller;

import com.safayet.afsos_nama.dto.ProfileDTO;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.service.UserService;
import com.safayet.afsos_nama.service.PilotAccess;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final PilotAccess pilotAccess;
    private final JdbcTemplate jdbc;

    @GetMapping(value = "/profile/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> export(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        int userId = user.getId();
        Map<String, Object> data = Map.of(
                "profile", userService.getProfile(user),
                "reflections", jdbc.queryForList("""
                        SELECT id, title, description, category, level, regret_date, related_course, semester,
                               consequence, lesson_learned, status, image_url, shared_anonymously, approved_for_feed
                        FROM afsos WHERE user_id = ? ORDER BY id
                        """, userId),
                "promises", jdbc.queryForList("""
                        SELECT id, title, target_date, status, broken_count, semester, source_afsos_id
                        FROM student_promises WHERE user_id = ? ORDER BY id
                        """, userId),
                "reactions", jdbc.queryForList("""
                        SELECT afsos_id, reaction_type FROM afsos_reactions WHERE user_id = ? ORDER BY afsos_id
                        """, userId));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("amolnama-data.json").build().toString())
                .body(data);
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = userService.getUserByEmail(authentication.getName());
        model.addAttribute("privacyContact", pilotAccess.privacyContact());
        model.addAttribute("profileDTO", userService.getProfile(user));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileDTO") ProfileDTO profileDTO,
            BindingResult bindingResult,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (profileDTO.getCurrentCgpa() != null
                && profileDTO.getTargetCgpa() != null
                && profileDTO.getTargetCgpa().compareTo(profileDTO.getCurrentCgpa()) < 0) {
            bindingResult.rejectValue(
                    "targetCgpa",
                    "targetCgpa.lowerThanCurrent",
                    "Target CGPA cannot be lower than current CGPA"
            );
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("privacyContact", pilotAccess.privacyContact());
            return "profile";
        }

        User user = userService.getUserByEmail(authentication.getName());
        userService.updateProfile(user, profileDTO);
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile updated successfully."
        );

        return "redirect:/profile";
    }
}
