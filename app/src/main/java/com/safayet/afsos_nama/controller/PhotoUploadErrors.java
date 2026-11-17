package com.safayet.afsos_nama.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class PhotoUploadErrors {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", "Choose a JPEG or PNG photo smaller than 2 MB.");
        return "redirect:/profile";
    }
}
