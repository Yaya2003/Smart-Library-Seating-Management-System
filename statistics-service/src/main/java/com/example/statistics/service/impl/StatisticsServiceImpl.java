package com.example.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.po.Reservation;
import com.example.domain.po.User;
import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.TrendPointDTO;
import com.example.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final com.example.mapper.ReservationMapper reservationMapper;
    private final com.example.mapper.UserMapper userMapper;

    @Override
    public OverviewStatisticsDTO buildOverviewStatistics() {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        // 今日预约总数
        long todayReservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, todayStr)
        );

        // 今日已入座
        long todayCheckedInCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, todayStr)
                        .eq(Reservation::getStatus, ReservationStatus.CHECKED_IN.name())
        );

        // 当前在座人数：状态 CHECKED_IN 且未过期
        LocalDateTime now = LocalDateTime.now();
        long currentInSeatCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, todayStr)
                        .eq(Reservation::getStatus, ReservationStatus.CHECKED_IN.name())
                        .gt(Reservation::getExpiresAt, now)
        );

        // 今日违约次数：状态 BREACH
        long todayViolationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, todayStr)
                        .eq(Reservation::getStatus, ReservationStatus.BREACH.name())
        );

        OverviewStatisticsDTO dto = new OverviewStatisticsDTO();
        dto.setTodayReservationCount(todayReservationCount);
        dto.setTodayCheckedInCount(todayCheckedInCount);
        dto.setCurrentInSeatCount(currentInSeatCount);
        dto.setTodayViolationCount(todayViolationCount);
        return dto;
    }

    @Override
    public List<TrendPointDTO> buildReservationTrend(LocalDate date) {
        String dateStr = date.toString();
        List<TrendPointDTO> result = new ArrayList<>();

        // 改为按预约的时间段统计（上午/下午/晚上），而不是按下单时间
        long morning = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "MORNING")
        );
        long afternoon = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "AFTERNOON")
        );
        long evening = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "EVENING")
        );

        result.add(new TrendPointDTO("上午", morning));
        result.add(new TrendPointDTO("下午", afternoon));
        result.add(new TrendPointDTO("晚上", evening));

        return result;
    }

    @Override
    public List<TrendPointDTO> buildTimeSlotDistribution(LocalDate date) {
        String dateStr = date.toString();
        List<TrendPointDTO> result = new ArrayList<>();

        long morning = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "MORNING")
        );
        long afternoon = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "AFTERNOON")
        );
        long evening = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, dateStr)
                        .eq(Reservation::getTimeSlot, "EVENING")
        );

        result.add(new TrendPointDTO("上午", morning));
        result.add(new TrendPointDTO("下午", afternoon));
        result.add(new TrendPointDTO("晚上", evening));

        return result;
    }
}
