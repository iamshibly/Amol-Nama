package com.shibly.afsos_nama.service;

import com.shibly.afsos_nama.dto.DashboardDTO;
import com.shibly.afsos_nama.model.Afsos;
import com.shibly.afsos_nama.model.StudentPromise;
import com.shibly.afsos_nama.model.User;
import com.shibly.afsos_nama.model.enums.AfsosCategory;
import com.shibly.afsos_nama.model.enums.AfsosLevel;
import com.shibly.afsos_nama.model.enums.AfsosStatus;
import com.shibly.afsos_nama.model.enums.PromiseStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AfsosService afsosService;
    private final PromiseService promiseService;

    public SemesterView semesterView(User user, String requestedSemester) {
        var choices = new TreeSet<String>();
        choices.addAll(afsosService.getSemesters(user));
        choices.addAll(promiseService.getSemesters(user));
        choices.removeIf(String::isBlank);
        String current = user.getCurrentSemester();
        if (current != null && !current.isBlank()) {
            choices.add(current);
        }
        String semester = requestedSemester == null ? current : requestedSemester;
        if (semester != null && !choices.contains(semester)) {
            semester = null;
        }
        var reflections = afsosService.getAllAfsos(user, semester);
        var promises = promiseService.getAllPromises(user, semester);
        return new SemesterView(semester, List.copyOf(choices), reflections, summarize(reflections, promises));
    }

    private DashboardDTO summarize(List<Afsos> reflections, List<StudentPromise> promises) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        var recent = reflections.stream()
                .filter(entry -> !entry.getRegretDate().isBefore(start) && !entry.getRegretDate().isAfter(today))
                .toList();
        var categories = categoryBreakdown(reflections);
        var weeklyCategories = categoryBreakdown(recent);
        var active = promises.stream().filter(promise -> promise.getStatus() == PromiseStatus.ACTIVE).toList();
        long completed = promises.stream().filter(promise -> promise.getStatus() == PromiseStatus.COMPLETED).count();
        long broken = promises.stream().filter(promise -> promise.getStatus() == PromiseStatus.BROKEN).count();
        String repeated = categories.entrySet().stream().findFirst().filter(entry -> entry.getValue() >= 3)
                .map(entry -> entry.getKey() + " came up " + entry.getValue() + " times. Try one small change in this area.")
                .orElse("");
        return new DashboardDTO(
                reflections.size(),
                recent.size(),
                firstCategory(categories),
                reflections.stream().map(Afsos::getLevel).max(Comparator.comparingInt(AfsosLevel::getScore))
                        .map(AfsosLevel::getLabel).orElse("No data yet"),
                progressScore(reflections, promises.size(), completed, broken),
                active.size(),
                broken,
                repeated,
                reflections.stream().limit(5).toList(),
                active.stream().limit(4).toList(),
                categories,
                firstCategory(weeklyCategories),
                recent.stream().filter(DashboardService::resolved).findFirst().orElse(null)
        );
    }

    private Integer progressScore(List<Afsos> reflections, int promises, long completed, long broken) {
        if (reflections.isEmpty() && promises == 0) {
            return null;
        }
        long resolved = reflections.stream().filter(DashboardService::resolved).count();
        long highImpact = reflections.stream().filter(entry -> !resolved(entry))
                .filter(entry -> entry.getLevel().getScore() >= AfsosLevel.HIGH.getScore()).count();
        double score = 50 + 20 * ratio(completed, promises) + 30 * ratio(resolved, reflections.size())
                - 30 * ratio(highImpact, reflections.size()) - 20 * ratio(broken, promises);
        return (int) Math.round(Math.clamp(score, 0.0, 100.0));
    }

    private double ratio(long count, long total) {
        return total == 0 ? 0.0 : (double) count / total;
    }

    private static boolean resolved(Afsos entry) {
        return entry.getStatus() == AfsosStatus.LESSON_LEARNED || entry.getStatus() == AfsosStatus.FIXED;
    }

    private String firstCategory(Map<String, Long> categories) {
        return categories.keySet().stream().findFirst().orElse("No data yet");
    }

    private Map<String, Long> categoryBreakdown(List<Afsos> reflections) {
        var counts = reflections.stream().collect(Collectors.groupingBy(
                Afsos::getCategory, Collectors.counting()));
        var sorted = new LinkedHashMap<String, Long>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<AfsosCategory, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getLabel()))
                .forEach(entry -> sorted.put(entry.getKey().getLabel(), entry.getValue()));
        return sorted;
    }

    public record SemesterView(String selected, List<String> choices, List<Afsos> reflections, DashboardDTO data) {}
}
