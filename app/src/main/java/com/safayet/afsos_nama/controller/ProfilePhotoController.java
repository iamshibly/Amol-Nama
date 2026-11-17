package com.safayet.afsos_nama.controller;

import com.safayet.afsos_nama.service.ProfilePhotoService;
import com.safayet.afsos_nama.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ProfilePhotoController {
    private final UserService users;
    private final ProfilePhotoService photos;

    @GetMapping("/profile/photo")
    public ResponseEntity<byte[]> photo(Authentication authentication) {
        byte[] photo = photos.get(users.getUserByEmail(authentication.getName()).getId());
        if (photo == null) {
            return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.IMAGE_JPEG).body(photo);
    }

    @PostMapping("/profile/photo")
    public String upload(@RequestParam("photo") MultipartFile photo, Authentication authentication,
                         RedirectAttributes redirect) {
        try {
            photos.upload(users.getUserByEmail(authentication.getName()).getId(), photo);
            redirect.addFlashAttribute("successMessage", "Profile photo updated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/photo/remove")
    public String remove(Authentication authentication, RedirectAttributes redirect) {
        photos.remove(users.getUserByEmail(authentication.getName()).getId());
        redirect.addFlashAttribute("successMessage", "Profile photo removed.");
        return "redirect:/profile";
    }
}
