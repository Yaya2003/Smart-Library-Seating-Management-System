package com.example.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName(value = "reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long reservationId;

    private Long userId;

    /**
     * 新版预约基于座位而非房间行列。兼容旧字段，保留 roomId/row/column 用于历史记录
     */
    @TableField("seat_id")
    private Long seatId;
    private Long roomId;

    @TableField("room_name")
    private String roomName;

    @TableField("seat_name")
    private String seatName;

    /**
     * 预约日期（yyyy-MM-dd）
     */
    private String date;

    /**
     * 旧字段：具体时间字符串。新逻辑使用 timeSlot 枚举 morning/afternoon/evening
     */
    private String time;

    /**
     * 时间段枚举：MORNING / AFTERNOON / EVENING
     */
    @TableField("time_slot")
    private String timeSlot;

    /**
     * 预约状态：PENDING/CHECKED_IN/RELEASED/BREACH/CANCELLED
     */
    private String status;

    @TableField("checkin_at")
    private LocalDateTime checkinAt;

    /**
     * 预约开始后30分钟的超时时间，用于自动释放
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField(value = "`row`")
    private Integer row;
    @TableField(value = "`column`")
    private Integer column;

    @TableField("created_at")
    private Date createAt;
    @TableField("updated_at")
    private Date updateAt;

    /**
     * 以下字段为管理端查询过滤使用，不直接映射数据库列
     */
    @TableField(exist = false)
    private Integer age;

    @TableField(exist = false)
    private String gender;

    @TableField(exist = false)
    private String className;

    /**
     * 学院（用户所属院系/部门）
     */
    @TableField(exist = false)
    private String department;

    @TableField(exist = false)
    private String phone;

    @TableField(exist = false)
    private String email;
}
