package com.example.statistics.dto;

import lombok.Data;

/**
 * 签到转化率数据点。
 */
@Data
public class CheckinConversionDTO {

    /**
     * 维度标签：时间段名称 / 阅览室名称等。
     */
    private String label;

    /**
     * 有效预约次数（非 CANCELLED）。
     */
    private long reservationCount;

    /**
     * 已签到次数。
     */
    private long checkedInCount;

    /**
     * 签到率 = 已签到 ÷ 有效预约。
     */
    private double checkinRate;
}

