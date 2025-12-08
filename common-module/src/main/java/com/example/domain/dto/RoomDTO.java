package com.example.domain.dto;

import lombok.Data;

@Data
public class RoomDTO {
    private Integer floor;
    private String roomName;
    private Integer row;
    private Integer column;
    private String state;
}
