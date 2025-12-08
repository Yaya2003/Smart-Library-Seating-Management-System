package com.example.statistics.dto;

import lombok.Data;

/**
 * 每日违约 / 取消趋势点。
 */
@Data
public class ViolationCancelTrendDTO {

    /**
     * 日期字符串：yyyy-MM-dd。
     */
    private String date;

    /**
     * 当日违约次数（BREACH）。
     */
    private long breachCount;

    /**
     * 当日取消次数（CANCELLED）。
     */
    private long cancelCount;

    /**
     * 违约率 = 违约次数 ÷ 有效预约次数（非 CANCELLED）。
     */
    private double violationRate;
}

