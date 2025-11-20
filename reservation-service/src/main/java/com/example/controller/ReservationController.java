package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.result.ResultView;
import com.example.common.session.UserSession;
import com.example.domain.po.Reservation;
import com.example.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservation")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/saveReservation")
    public ResultView<?> saveReservation(@RequestBody @Valid Reservation reservation) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        reservation.setUserId(id);
        synchronized (this) {
            Reservation one = reservationService.getOne(new LambdaQueryWrapper<Reservation>()
                    .eq(Reservation::getRoomId, reservation.getRoomId())
                    .eq(Reservation::getRow, reservation.getRow())
                    .eq(Reservation::getColumn, reservation.getColumn())
                    .eq(Reservation::getTime, reservation.getTime())
                    .eq(Reservation::getDate, reservation.getDate()));
            if (one != null) {
                return ResultView.error(HttpStatus.BAD_REQUEST.value(), "该时间段已有预约，请选择其他时间段");
            }
            reservationService.save(reservation);
        }
        return ResultView.success(true);
    }

    @DeleteMapping("/deleteReservation")
    public ResultView<?> deleteReservation(@RequestBody @Valid Reservation reservation) {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = userSession.getUserId();
        reservation.setUserId(id);
        reservationService.removeReservation(reservation);
        return ResultView.success(true);
    }

    @GetMapping("/getReservationList")
    public ResultView<?> getReservationList() {
        UserSession userSession = (UserSession) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userSession.getUserId();
        return ResultView.success(reservationService.getReservation(userId));
    }
}
