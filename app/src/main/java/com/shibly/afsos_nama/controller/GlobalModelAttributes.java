package com.shibly.afsos_nama.controller;

import com.shibly.afsos_nama.model.User;
import com.shibly.afsos_nama.model.enums.Role;
import com.shibly.afsos_nama.service.UserService;
import com.shibly.afsos_nama.service.ProfilePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final ProfilePhotoService profilePhotos;

    @ModelAttribute
    public void addCurrentUser(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }

        User user = userService.getUserByEmail(authentication.getName());

        if (user != null) {
            model.addAttribute("currentUser", user);
            model.addAttribute("hasProfilePhoto", profilePhotos.exists(user.getId()));
            model.addAttribute("isAdmin", user.getRole() == Role.ADMIN);
        }
    }
}
