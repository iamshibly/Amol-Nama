package com.shibly.afsos_nama.dto;

import com.shibly.afsos_nama.model.Afsos;
import com.shibly.afsos_nama.model.StudentPromise;

import java.util.List;
import java.util.Map;

public record DashboardDTO(
        long totalAfsos,
        long thisWeek,
        String mostCommonCategory,
        String highestLevel,
        Integer progressScore,
        long activePromises,
        long brokenPromises,
        String repeatedWarning,
        List<Afsos> recentAfsos,
        List<StudentPromise> upcomingPromises,
        Map<String, Long> categoryBreakdown,
        String weeklyCategory,
        Afsos weeklyResolved
) {}
