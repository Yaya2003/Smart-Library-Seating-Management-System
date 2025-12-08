package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.po.Seat;

public interface SeatService extends IService<Seat> {
    /**
     * 刷新座位二维码令牌
     */
    String refreshQrToken(Long seatId);

    /**
     * 启用/停用座位
     */
    void updateStatus(Long seatId, Integer status);

    /**
     * 保存或更新座位，带唯一性校验与默认值
     */
    void saveSeatWithValidate(Seat seat);
}
