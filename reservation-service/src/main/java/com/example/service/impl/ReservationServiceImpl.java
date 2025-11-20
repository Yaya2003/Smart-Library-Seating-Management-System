package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.domain.po.Reservation;
import com.example.domain.po.Room;
import com.example.domain.vo.ReservationVO;
import com.example.mapper.ReservationMapper;
import com.example.mapper.RoomMapper;
import com.example.service.ReservationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Override
    public void removeReservation(Reservation reservation) {
        reservationMapper.deleteReservation(reservation);
    }

    @Override
    public List<ReservationVO> getReservation(Long userId) {
        List<ReservationVO> reservationVOList = new ArrayList<>();
        List<Reservation> reservations = this.list(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId));
        for (Reservation reservation : reservations) {
            String[] time = reservation.getDate().split("-");

            LocalDate date = LocalDate.of(
                    Integer.parseInt(time[0]),
                    Integer.parseInt(time[1]),
                    Integer.parseInt(time[2])
            );

            if (date.isBefore(LocalDate.now())) {
                continue;
            }

            ReservationVO reservationVO = new ReservationVO();
            Room room = roomMapper.selectById(reservation.getRoomId());
            BeanUtils.copyProperties(room, reservationVO);
            BeanUtils.copyProperties(reservation, reservationVO);
            reservationVOList.add(reservationVO);
        }
        return reservationVOList;
    }
}
