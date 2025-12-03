package com.example.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusMessage {
    private String type = "seatStatus";
    private Long roomId;
    private Long seatId;
    private Integer row;
    private Integer column;
    private Long userId;
    private String date;
    private String timeSlot;
    private String status;

    public SeatStatusMessage(Long roomId, Long seatId, Integer row, Integer column, Long userId, String date, String timeSlot, String status) {
        this.roomId = roomId;
        this.seatId = seatId;
        this.row = row;
        this.column = column;
        this.userId = userId;
        this.date = date;
        this.timeSlot = timeSlot;
        this.status = status;
    }
}

