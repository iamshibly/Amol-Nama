package com.safayet.afsos_nama;

import com.safayet.afsos_nama.controller.AuthController;
import com.safayet.afsos_nama.dto.RegistrationDTO;
import com.safayet.afsos_nama.service.PilotAccess;
import com.safayet.afsos_nama.service.UserService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private RegistrationDTO registration() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setName("Public Student");
        dto.setEmail("new-student@example.test");
        dto.setPassword("a-valid-password-123");
        dto.setConfirmPassword("a-valid-password-123");
        return dto;
    }

    @Test
    void publicSignupRequiresNoInvitationOrAllowlist() {
        RegistrationDTO dto = registration();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(dto).isEmpty());
        }
        UserService users = mock(UserService.class);
        AuthController controller = new AuthController(users, new PilotAccess(""));
        var result = new BeanPropertyBindingResult(dto, "registrationDTO");
        assertEquals("redirect:/login", controller.register(dto, result, new RedirectAttributesModelMap()));
        verify(users).register(dto);
        assertFalse(result.hasErrors());
    }

    @Test
    void publicSignupStillRejectsMismatchedPasswords() {
        RegistrationDTO dto = registration();
        dto.setConfirmPassword("a-different-password");
        UserService users = mock(UserService.class);
        AuthController controller = new AuthController(users, new PilotAccess(""));
        var result = new BeanPropertyBindingResult(dto, "registrationDTO");
        assertEquals("auth/register", controller.register(dto, result, new RedirectAttributesModelMap()));
        assertTrue(result.hasFieldErrors("confirmPassword"));
        verifyNoInteractions(users);
    }
}
