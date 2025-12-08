package com.example.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ReservationVO {
    private Long reservationId;

    private Long userId;

    private String userName;

    private Integer age;

    private String gender;

    private String className;

    /**
     * 学院（用户所属院系/部门）
     */
    private String department;

    private String phone;

    private String email;

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
