package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.session.UserSession;
import com.example.domain.po.Reservation;
import com.example.domain.vo.ReservationVO;
import jakarta.validation.Valid;

import java.util.List;

public interface ReservationService extends IService<Reservation> {
    boolean createReservation(@Valid Reservation reservation, UserSession userSession);

    void cancelReservation(Long reservationId, UserSession userSession, boolean isAdmin);

    void checkIn(Long reservationId, Long seatId, String qrToken, UserSession userSession);

    List<ReservationVO> getReservation(Long userId);

    /**
     * 管理端查询预约列表（可带过滤�?
     */
    List<ReservationVO> listAll(Reservation probe);

    /**
     * 管理端保�?编辑预约
     */
    void saveByAdmin(@Valid Reservation reservation, UserSession userSession);

    /**
     * 管理端删除预�?
     */
    void deleteByAdmin(Long reservationId);

    /**
     * 管理端批量删除预约
     */
    void batchDeleteByAdmin(List<Long> reservationIds);

    /**
     * �?量取消或释�?
     */
    void batchUpdateStatus(List<Long> ids, String targetStatus, UserSession userSession, boolean isAdmin);

    /**
     * 违约次数
     */
    int getViolationCount(Long userId);
}
