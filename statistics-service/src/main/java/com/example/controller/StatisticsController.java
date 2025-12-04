package com.example.controller;

import com.example.common.result.ResultView;
import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.TrendPointDTO;
import com.example.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 首页概览统计：
     * - 今日预约数量
     * - 今日已入座人数
     * - 当前在座人数
     * - 今日违约次数
     */
    @GetMapping("/overview")
    public ResultView<OverviewStatisticsDTO> overview() {
        OverviewStatisticsDTO dto = statisticsService.buildOverviewStatistics();
        return ResultView.success(dto);
    }

    /**
     * 按时间段统计某天预约趋势，用于折线图。
     */
    @GetMapping("/reservation-trend")
    public ResultView<List<TrendPointDTO>> reservationTrend(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TrendPointDTO> trend = statisticsService.buildReservationTrend(date != null ? date : LocalDate.now());
        return ResultView.success(trend);
    }

    /**
     * 今日时间段占比（上午/下午/晚上），用于饼图。
     */
    @GetMapping("/time-slot-distribution")
    public ResultView<List<TrendPointDTO>> timeSlotDistribution() {
        List<TrendPointDTO> distribution = statisticsService.buildTimeSlotDistribution(LocalDate.now());
        return ResultView.success(distribution);
    }
}
