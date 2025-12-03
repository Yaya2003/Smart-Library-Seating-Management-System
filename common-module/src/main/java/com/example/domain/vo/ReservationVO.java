package com.example.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ReservationVO {
    private Long reservationId;

    private Long userId;

    private String userName;

    private Long roomId;

    private Integer floor;

    private String roomName;

    /**
     * 当前用户违约次数
     */
    private Integer violationCount;

    private Integer row;

    private Integer column;

    private Long seatId;

    private String seatName;

    private String date;

    private String time;

    private String timeSlot;

    private String status;

    private Date createAt;

    private Date updateAt;
}
