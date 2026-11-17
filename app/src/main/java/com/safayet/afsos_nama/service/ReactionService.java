package com.safayet.afsos_nama.service;

import com.safayet.afsos_nama.model.Afsos;
import com.safayet.afsos_nama.model.enums.ReactionType;
import com.safayet.afsos_nama.repository.AfsosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final JdbcTemplate jdbc;
    private final AfsosRepository afsosRepository;

    @Transactional
    public void setReaction(int afsosId, int userId, ReactionType reaction) {
        Afsos post = afsosRepository.findForUpdate(afsosId).orElse(null);
        if (post == null || !post.isSharedAnonymously() || !post.isApprovedForFeed()) {
            return;
        }
        if (reaction == null) {
            jdbc.update("DELETE FROM afsos_reactions WHERE afsos_id = ? AND user_id = ?", afsosId, userId);
        } else {
            jdbc.update("""
                    INSERT INTO afsos_reactions (afsos_id, user_id, reaction_type)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE reaction_type = ?
                    """, afsosId, userId, reaction.name(), reaction.name());
        }
    }

    public Map<Integer, Map<String, ReactionCount>> summaries(List<Afsos> posts, int userId) {
        Map<Integer, Map<String, ReactionCount>> result = new HashMap<>();
        if (posts.isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(posts.size(), "?"));
        Object[] parameters = new Object[posts.size() + 1];
        parameters[0] = userId;
        for (int i = 0; i < posts.size(); i++) {
            parameters[i + 1] = posts.get(i).getId();
        }
        jdbc.query("""
                SELECT afsos_id, reaction_type, COUNT(*) AS total,
                       MAX(CASE WHEN user_id = ? THEN 1 ELSE 0 END) AS selected
                FROM afsos_reactions WHERE afsos_id IN (%s)
                GROUP BY afsos_id, reaction_type
                """.formatted(placeholders), rs -> {
            result.computeIfAbsent(rs.getInt("afsos_id"), key -> new HashMap<>())
                    .put(rs.getString("reaction_type"),
                            new ReactionCount(rs.getLong("total"), rs.getBoolean("selected")));
        }, parameters);
        return result;
    }

    public record ReactionCount(long total, boolean selected) {}
}
