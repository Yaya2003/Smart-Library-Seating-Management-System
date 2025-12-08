package com.example.domain.enums;

import java.time.LocalTime;

/**
 * 时间段枚举，带起止时间。
 */
public enum TimeSlot {
    MORNING(LocalTime.of(8, 0), LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(12, 30), LocalTime.of(17, 30)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(22, 0));

    private final LocalTime start;
    private final LocalTime end;

    TimeSlot(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}
