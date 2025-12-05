package com.example.statistics.dto;

import lombok.Data;

/**
 * 阅览室 x 时间段 热力图单元格。
 */
@Data
public class RoomTimeSlotHeatDTO {

    private Long roomId;

    private String roomName;

    /**
     * 时间段：MORNING / AFTERNOON / EVENING。
     */
    private String timeSlot;

    /**
     * 区间内预约总数（排除已取消）。
     */
    private long reservationCount;

    /**
     * 区间内已签到次数。
     */
    private long checkedInCount;

    /**
     * 利用率 = 已签到 ÷ 可用座位数。
     */
    private double utilizationRate;
}

