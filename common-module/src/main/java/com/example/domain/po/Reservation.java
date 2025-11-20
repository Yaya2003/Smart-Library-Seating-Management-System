package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long reservationId;
    private Long userId;
    private Long roomId;
    private String date;
    private String time;
    @TableField(value = "`row`")
    private Integer row;
    @TableField(value = "`column`")
    private Integer column;

    private Date createAt;
    private Date updateAt;
}
