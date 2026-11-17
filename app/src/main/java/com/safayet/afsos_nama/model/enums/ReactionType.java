package com.safayet.afsos_nama.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReactionType {
    SAME_BHAI("Same, bhai", "blue", "users"),
    RIP_CGPA("RIP CGPA", "coral", "alert"),
    NEXT_SEMESTER("Next semester", "amber", "repeat");

    private final String label;
    private final String tone;
    private final String icon;
}
