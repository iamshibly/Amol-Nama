package com.shibly.afsos_nama.service;

import com.shibly.afsos_nama.dto.ProfileDTO;
import com.shibly.afsos_nama.dto.RegistrationDTO;
import com.shibly.afsos_nama.model.User;
import com.shibly.afsos_nama.model.enums.Role;
import com.shibly.afsos_nama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public void register(RegistrationDTO dto) {
        String email = PilotAccess.normalizeEmail(dto.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account already exists with this email");
        }

        User user = new User();
        BeanUtils.copyProperties(dto, user, "password", "confirmPassword");
        normalizeSemester(user);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(PilotAccess.normalizeEmail(email)).orElse(null);
    }

    public ProfileDTO getProfile(User user) {
        ProfileDTO dto = new ProfileDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    @Transactional
    public void updateProfile(User user, ProfileDTO dto) {
        User current = userRepository.findForUpdate(user.getId()).orElseThrow();
        BeanUtils.copyProperties(dto, current, "email");
        normalizeSemester(current);
    }

    private void normalizeSemester(User user) {
        String semester = user.getCurrentSemester();
        user.setCurrentSemester(semester == null || semester.isBlank() ? null : semester.strip());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public boolean toggleUser(Integer id, String actorEmail) {
        User user = userRepository.findForUpdate(id).orElse(null);

        if (user != null && user.getRole() != Role.ADMIN) {
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            if (!user.isEnabled()) {
                jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?", user.getEmail());
            }
            jdbc.update("INSERT INTO moderation_audit(actor_user_id, action, target_id) VALUES (?, ?, ?)",
                    getUserByEmail(actorEmail).getId(), user.isEnabled() ? "ENABLE_USER" : "DISABLE_USER", id);
            return true;
        }
        return false;
    }

    public long countUsers() {
        return userRepository.count();
    }
}
