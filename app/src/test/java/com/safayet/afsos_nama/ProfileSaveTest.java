package com.safayet.afsos_nama;

import com.safayet.afsos_nama.dto.RegistrationDTO;
import com.safayet.afsos_nama.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
class ProfileSaveTest {
    @Autowired WebApplicationContext context;
    @Autowired UserService users;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    String email;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        email = "profile-" + UUID.randomUUID() + "@example.test";
        var dto = new RegistrationDTO();
        dto.setName("Profile Test");
        dto.setEmail(email);
        dto.setPassword("profile-test-password");
        dto.setConfirmPassword("profile-test-password");
        users.register(dto);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM users WHERE email = ?", email);
    }

    @Test
    void saveWithBlankCgpaAndThenWithCgpaPersistsAndRenders() throws Exception {
        mvc.perform(post("/profile").with(user(email)).with(csrf())
                .param("name", "Updated Student").param("email", email)
                .param("university", "Southeast University")
                .param("department", "Computer Science and Engineering")
                .param("currentSemester", "12th").param("currentCgpa", "").param("targetCgpa", ""))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile"));
        assertEquals("Updated Student", users.getUserByEmail(email).getName());
        assertEquals("12th", users.getUserByEmail(email).getCurrentSemester());
        assertNull(users.getUserByEmail(email).getCurrentCgpa());
        mvc.perform(get("/profile").with(user(email)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Updated Student")));
        mvc.perform(post("/profile").with(user(email)).with(csrf())
                .param("name", "Updated Again").param("email", email)
                .param("currentCgpa", "3.50").param("targetCgpa", "3.80"))
                .andExpect(status().is3xxRedirection());
        assertEquals(new java.math.BigDecimal("3.50"), users.getUserByEmail(email).getCurrentCgpa());
    }

    @Test
    void errorPageActuallyRendersTheDiagnosticPanel() throws Exception {
        mvc.perform(get("/error").accept(org.springframework.http.MediaType.TEXT_HTML)
                .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 400))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error details")))
                .andExpect(content().string(containsString("D2")))
                .andExpect(content().string(containsString("Reference:")));
    }

    @Test
    void invalidInputRendersFieldErrorsAndKeepsExistingData() throws Exception {
        mvc.perform(post("/profile").with(user(email)).with(csrf())
                .param("name", "Should Not Save").param("email", email)
                .param("currentCgpa", "3.80").param("targetCgpa", "3.50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Target CGPA cannot be lower than current CGPA")));
        assertEquals("Profile Test", users.getUserByEmail(email).getName());
    }
}
