package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.po.Reservation;
import com.example.domain.vo.ReservationVO;
import jakarta.validation.Valid;

import java.util.List;

public interface ReservationService extends IService<Reservation> {
    void removeReservation(@Valid Reservation reservation);

    List<ReservationVO> getReservation(Long userId);
}
