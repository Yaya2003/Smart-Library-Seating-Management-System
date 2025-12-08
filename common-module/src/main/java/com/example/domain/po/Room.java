package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.gson.StringToInteger2DTypeAdaptor;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long roomId;
    private String roomName;
    private Integer floor;
    @TableField(value = "`row`")
    private Integer row;
    @TableField(value = "`column`")
    private Integer column;

    @JsonAdapter(value = StringToInteger2DTypeAdaptor.class)
    private String seatInfo;
    private String state;
    private Date createAt;
    private Date updateAt;
}
