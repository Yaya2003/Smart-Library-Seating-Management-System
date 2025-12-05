package com.example.statistics.dto;

import lombok.Data;

/**
 * 阅览室利用率排行条目。
 */
@Data
public class RoomUsageRankDTO {

    private Long roomId;

    private String roomName;

    /**
     * 统计区间内的预约总数（排除已取消）。
     */
    private long reservationCount;

    /**
     * 实际签到次数。
     */
    private long checkedInCount;

    /**
     * 可用座位数。
     */
    private long seatCount;

    /**
     * 占用率 = 已签到 ÷ 可用座位数。
     */
    private double occupancyRate;
}

