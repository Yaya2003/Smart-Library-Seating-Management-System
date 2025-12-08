package com.example.statistics.dto;

import lombok.Data;

/**
 * 学院 / 班级维度统计。
 */
@Data
public class DeptClassStatisticsDTO {

    /**
     * 学院名称。
     */
    private String department;

    /**
     * 班级名称。
     */
    private String className;

    /**
     * 预约总数（排除已取消）。
     */
    private long reservationCount;

    /**
     * 违约次数（BREACH）。
     */
    private long breachCount;

    /**
     * 违约率 = 违约 ÷ 有效预约。
     */
    private double violationRate;
}

