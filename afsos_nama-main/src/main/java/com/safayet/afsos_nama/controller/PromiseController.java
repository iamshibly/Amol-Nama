package com.safayet.afsos_nama.controller;

import com.safayet.afsos_nama.dto.PromiseDTO;
import com.safayet.afsos_nama.model.StudentPromise;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.model.enums.PromiseStatus;
import com.safayet.afsos_nama.service.PromiseService;
import com.safayet.afsos_nama.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/promises")
public class PromiseController {

    private final PromiseService promiseService;
    private final UserService userService;

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute(
                "promises",
                promiseService.getAllPromises(currentUser(authentication))
        );
        return "promises/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        PromiseDTO dto = new PromiseDTO();
        dto.setTargetDate(LocalDate.now().plusDays(7));
        dto.setStatus(PromiseStatus.ACTIVE);

        model.addAttribute("promiseDTO", dto);
        return "promises/form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("promiseDTO") PromiseDTO promiseDTO,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User user = currentUser(authentication);
        StudentPromise existing = promiseDTO.getId() == null
                ? null
                : promiseService.getPromiseById(promiseDTO.getId(), user);

        if (promiseDTO.getId() != null && existing == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Promise not found or does not belong to you.");
            return "redirect:/promises";
        }

        validateTargetDate(promiseDTO, existing, bindingResult);

        if (existing == null) {
            promiseDTO.setStatus(PromiseStatus.ACTIVE);
        } else {
            promiseDTO.setStatus(existing.getStatus());
        }

        if (bindingResult.hasErrors()) {
            return "promises/form";
        }

        boolean saved = promiseService.savePromise(promiseDTO, user);

        if (!saved) {
            redirectAttributes.addFlashAttribute("errorMessage", "Promise could not be saved. Check the target date and try again.");
            return "redirect:/promises";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Promise saved.");
        return "redirect:/promises";
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        StudentPromise promise = promiseService.getPromiseById(
                id,
                currentUser(authentication)
        );

        if (promise == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Promise not found.");
            return "redirect:/promises";
        }

        PromiseDTO dto = new PromiseDTO();
        BeanUtils.copyProperties(promise, dto);

        model.addAttribute("promiseDTO", dto);
        return "promises/form";
    }

    @PostMapping("/complete/{id}")
    public String complete(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean completed = promiseService.markCompleted(id, currentUser(authentication));
        redirectAttributes.addFlashAttribute(
                completed ? "successMessage" : "errorMessage",
                completed
                        ? "Promise completed. Character development unlocked."
                        : "Only an active promise can be completed."
        );
        return "redirect:/promises";
    }

    @PostMapping("/break/{id}")
    public String breakPromise(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean broken = promiseService.markBroken(id, currentUser(authentication));
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                broken
                        ? "Promise marked as broken. Kal theke abar try korben."
                        : "Only an active promise can be marked as broken."
        );
        return "redirect:/promises";
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        boolean deleted = promiseService.deletePromise(id, currentUser(authentication));
        redirectAttributes.addFlashAttribute(
                deleted ? "successMessage" : "errorMessage",
                deleted ? "Promise deleted." : "Promise not found or already deleted."
        );
        return "redirect:/promises";
    }

    private void validateTargetDate(
            PromiseDTO dto,
            StudentPromise existing,
            BindingResult bindingResult
    ) {
        if (dto.getTargetDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (existing == null) {
            if (dto.getTargetDate().isBefore(today)) {
                bindingResult.rejectValue(
                        "targetDate",
                        "targetDate.past",
                        "Target date cannot be in the past"
                );
            }
            return;
        }

        if (existing.getStatus() == PromiseStatus.BROKEN
                && dto.getTargetDate().isBefore(today)) {
            bindingResult.rejectValue(
                    "targetDate",
                    "targetDate.past",
                    "Choose today or a future date when revising a broken promise"
            );
            return;
        }

        if (existing.getStatus() == PromiseStatus.ACTIVE
                && dto.getTargetDate().isBefore(today)
                && !dto.getTargetDate().equals(existing.getTargetDate())) {
            bindingResult.rejectValue(
                    "targetDate",
                    "targetDate.past",
                    "Target date cannot be changed to another past date"
            );
        }
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }
}
