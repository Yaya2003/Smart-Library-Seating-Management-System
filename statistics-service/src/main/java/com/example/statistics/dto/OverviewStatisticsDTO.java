package com.example.statistics.dto;

import lombok.Data;

@Data
public class OverviewStatisticsDTO {

    /**
     * 今日预约总数
     */
    private long todayReservationCount;

    /**
     * 今日已入座人数
     */
    private long todayCheckedInCount;

    /**
     * 当前在座人数（状态为 CHECKED_IN 且未过期）
     */
    private long currentInSeatCount;

    /**
     * 今日违约次数
     */
    private long todayViolationCount;
}

