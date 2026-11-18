package com.shibly.afsos_nama.service;

import com.shibly.afsos_nama.dto.AfsosDTO;
import com.shibly.afsos_nama.model.Afsos;
import com.shibly.afsos_nama.model.User;
import com.shibly.afsos_nama.model.enums.AfsosCategory;
import com.shibly.afsos_nama.model.enums.AfsosLevel;
import com.shibly.afsos_nama.model.enums.AfsosStatus;
import com.shibly.afsos_nama.repository.AfsosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AfsosService {

    private final AfsosRepository afsosRepository;
    private final JdbcTemplate jdbc;

    @Transactional
    public void saveAfsos(AfsosDTO dto, User user) {
        Afsos afsos = dto.getId() == null
                ? new Afsos()
                : afsosRepository.findForUpdate(dto.getId()).orElse(null);

        if (afsos == null || (dto.getId() != null && !afsos.getUser().getId().equals(user.getId()))) {
            throw new IllegalArgumentException("Afsos not found");
        }

        boolean newAfsos = afsos.getId() == null;

        BeanUtils.copyProperties(
                dto,
                afsos,
                "id",
                "user",
                "approvedForFeed",
                "sameBhaiCount",
                "ripCgpaCount",
                "nextSemesterCount",
                "imageUrl"
        );

        if (newAfsos) {
            afsos.setUser(user);
        }

        if (afsos.getStatus() == null) {
            afsos.setStatus(AfsosStatus.NEW);
        }

        String semester = afsos.getSemester();
        afsos.setSemester(semester == null || semester.isBlank() ? user.getCurrentSemester() : semester.strip());

        afsos.setApprovedForFeed(false);

        afsosRepository.save(afsos);
    }

    public List<Afsos> getAllAfsos(User user) {
        return afsosRepository.findByUserOrderByRegretDateDesc(user);
    }

    public List<Afsos> getAllAfsos(User user, String semester) {
        return semester == null ? getAllAfsos(user)
                : afsosRepository.findByUserAndSemesterOrderByRegretDateDesc(user, semester);
    }

    public List<String> getSemesters(User user) {
        return afsosRepository.findSemesters(user);
    }

    public Page<Afsos> searchAfsos(User user, String keyword, AfsosCategory category,
                                  AfsosLevel level, int page) {
        String safeKeyword = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        return afsosRepository.search(user, safeKeyword, category, level, PageRequest.of(Math.max(0, page), 12));
    }

    public Afsos getAfsosById(Integer id, User user) {
        return afsosRepository.findByIdAndUser(id, user).orElse(null);
    }

    public void deleteAfsos(Integer id, User user) {
        Afsos afsos = getAfsosById(id, user);

        if (afsos != null) {
            afsosRepository.delete(afsos);
        }
    }

    public List<Afsos> getApprovedConfessions() {
        return afsosRepository
                .findBySharedAnonymouslyTrueAndApprovedForFeedTrueOrderByRegretDateDesc();
    }

    public List<Afsos> getPendingConfessions() {
        return afsosRepository
                .findBySharedAnonymouslyTrueAndApprovedForFeedFalseOrderByRegretDateDesc();
    }

    @Transactional
    public boolean moderateConfession(int id, User moderator, boolean approve) {
        Afsos afsos = afsosRepository.findForUpdate(id).orElse(null);
        if (afsos == null || !afsos.isSharedAnonymously() || (approve && afsos.isApprovedForFeed())) {
            return false;
        }
        afsos.setApprovedForFeed(approve);
        if (!approve) {
            afsos.setSharedAnonymously(false);
        }
        jdbc.update("INSERT INTO moderation_audit(actor_user_id, action, target_id) VALUES (?, ?, ?)",
                moderator.getId(), approve ? "APPROVE_CONFESSION" : "HIDE_CONFESSION", id);
        return true;
    }

    public long countAllAfsos() {
        return afsosRepository.count();
    }
}
