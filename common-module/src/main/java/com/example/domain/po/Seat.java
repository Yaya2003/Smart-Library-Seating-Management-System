package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 座位基础信息，用于二维码及启停控制
 */
@Data
@TableName(value = "seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("room_id")
    private Long roomId;

    @TableField("row_no")
    private Integer rowNo;

    @TableField("col_no")
    private Integer colNo;

    /**
     * 座位附加信息（JSON）
     */
    @TableField("seat_info")
    private String seatInfo;

    private String name;

    /**
     * 1 启用；0 停用
     */
    private Integer status;

    @TableField("qr_token")
    private String qrToken;

    @TableField("qr_updated_at")
    private Date qrUpdatedAt;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
