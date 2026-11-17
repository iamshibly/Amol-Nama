package com.safayet.afsos_nama.controller;

import com.safayet.afsos_nama.dto.AfsosDTO;
import com.safayet.afsos_nama.model.Afsos;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.model.enums.AfsosCategory;
import com.safayet.afsos_nama.model.enums.AfsosLevel;
import com.safayet.afsos_nama.model.enums.AfsosStatus;
import com.safayet.afsos_nama.service.AfsosService;
import com.safayet.afsos_nama.service.DashboardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/afsos")
public class AfsosController {

    private final AfsosService afsosService;
    private final UserService userService;
    private final DashboardService dashboardService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AfsosCategory category,
            @RequestParam(required = false) AfsosLevel level,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model
    )
    {
        User user = currentUser(authentication);

        var result = afsosService.searchAfsos(user, keyword, category, level, page);
        model.addAttribute("afsosList", result.getContent());
        model.addAttribute("afsosPage", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedLevel", level);
        addOptions(model);

        return "afsos/list";
    }

    @GetMapping("/new")
    public String createForm(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        AfsosDTO dto = new AfsosDTO();

        dto.setRegretDate(LocalDate.now());
        dto.setStatus(AfsosStatus.NEW);
        dto.setSemester(user.getCurrentSemester());

        model.addAttribute("afsosDTO", dto);
        addOptions(model);

        return "afsos/form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("afsosDTO") AfsosDTO afsosDTO,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (afsosDTO.getStatus() == AfsosStatus.LESSON_LEARNED
                && (afsosDTO.getLessonLearned() == null || afsosDTO.getLessonLearned().isBlank())) {
            bindingResult.rejectValue(
                    "lessonLearned",
                    "lessonLearned.required",
                    "Lesson learned is required when status is Lesson Learned"
            );
        }

        if (bindingResult.hasErrors()) {
            addOptions(model);
            return "afsos/form";
        }

        boolean isNew = afsosDTO.getId() == null;

        try {
            afsosService.saveAfsos(afsosDTO, currentUser(authentication));
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Afsos not found or cannot be updated.");
            return "redirect:/afsos";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                isNew ? "Afsos added successfully." : "Afsos updated successfully."
        );

        return "redirect:/afsos";
    }

    @GetMapping("/edit/{id}")
    public String edit(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Afsos afsos = afsosService.getAfsosById(id, currentUser(authentication));

        if (afsos == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Afsos not found.");
            return "redirect:/afsos";
        }

        AfsosDTO dto = new AfsosDTO();
        BeanUtils.copyProperties(afsos, dto);

        model.addAttribute("afsosDTO", dto);
        addOptions(model);

        return "afsos/form";
    }

    @GetMapping("/details/{id}")
    public String details(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Afsos afsos = afsosService.getAfsosById(id, currentUser(authentication));

        if (afsos == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Afsos not found.");
            return "redirect:/afsos";
        }

        model.addAttribute("afsos", afsos);
        return "afsos/details";
    }

    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = currentUser(authentication);
        Afsos afsos = afsosService.getAfsosById(id, user);

        if (afsos == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Afsos not found or already deleted.");
            return "redirect:/afsos";
        }

        afsosService.deleteAfsos(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Afsos deleted.");
        return "redirect:/afsos";
    }

    @GetMapping("/timeline")
    public String timeline(Authentication authentication, Model model) {
        model.addAttribute(
                "afsosList",
                afsosService.getAllAfsos(currentUser(authentication))
        );
        return "afsos/timeline";
    }

    @GetMapping("/report")
    public String report(@RequestParam(required = false) String semester, Authentication authentication, Model model) {
        User user = currentUser(authentication);

        var view = dashboardService.semesterView(user, semester);
        model.addAttribute("reportData", view.data());
        model.addAttribute("afsosList", view.reflections());
        model.addAttribute("semesterView", view);

        return "afsos/report";
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByEmail(authentication.getName());
    }

    private void addOptions(Model model) {
        model.addAttribute("categories", AfsosCategory.values());
        model.addAttribute("levels", AfsosLevel.values());
        model.addAttribute("statuses", AfsosStatus.values());
    }
}
