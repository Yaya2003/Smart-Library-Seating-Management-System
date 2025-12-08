package com.example.statistics.dto;

import lombok.Data;

/**
 * 座位使用频次统计。
 */
@Data
public class SeatUsageDTO {

    private Long seatId;

    private String seatName;

    private Long roomId;

    private String roomName;

    /**
     * 在统计区间内被预约次数。
     */
    private long reservationCount;
}

