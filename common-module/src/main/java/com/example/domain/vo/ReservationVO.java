package com.example.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ReservationVO {
    private Integer floor;

    private String roomName;

    private Integer row;

    private Integer column;

    private String date;

    private String time;

    private Date createAt;

    private Date updateAt;
}
