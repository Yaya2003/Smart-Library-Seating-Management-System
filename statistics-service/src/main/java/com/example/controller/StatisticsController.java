package com.example.controller;

import com.example.common.result.ResultView;
import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.TrendPointDTO;
import com.example.statistics.dto.RoomUsageRankDTO;
import com.example.statistics.dto.RoomTimeSlotHeatDTO;
import com.example.statistics.dto.ViolationCancelTrendDTO;
import com.example.statistics.dto.CheckinConversionDTO;
import com.example.statistics.dto.DeptClassStatisticsDTO;
import com.example.statistics.dto.SeatUsageDTO;
import com.example.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * 阅览室利用率排行：按日期范围统计预约数、签到数、占用率。
     */
    @GetMapping("/room-usage-rank")
    public ResultView<List<RoomUsageRankDTO>> roomUsageRank(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RoomUsageRankDTO> list = statisticsService.buildRoomUsageRank(startDate, endDate);
        return ResultView.success(list);
    }

    /**
     * 阅览室 x 时间段 热力图数据。
     */
    @GetMapping("/room-time-slot-heat")
    public ResultView<List<RoomTimeSlotHeatDTO>> roomTimeSlotHeat(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RoomTimeSlotHeatDTO> list = statisticsService.buildRoomTimeSlotHeat(startDate, endDate);
        return ResultView.success(list);
    }

    /**
     * 违约 / 取消趋势（近 N 天）。
     */
    @GetMapping("/violation-cancel-trend")
    public ResultView<List<ViolationCancelTrendDTO>> violationCancelTrend(
            @RequestParam(name = "days", defaultValue = "7") int days) {
        List<ViolationCancelTrendDTO> list = statisticsService.buildViolationCancelTrend(days);
        return ResultView.success(list);
    }

    /**
     * 按时间段统计签到率。
     */
    @GetMapping("/checkin-rate/time-slot")
    public ResultView<List<CheckinConversionDTO>> checkinRateByTimeSlot(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CheckinConversionDTO> list = statisticsService.buildCheckinRateByTimeSlot(startDate, endDate);
        return ResultView.success(list);
    }

    /**
     * 按阅览室统计签到率。
     */
    @GetMapping("/checkin-rate/room")
    public ResultView<List<CheckinConversionDTO>> checkinRateByRoom(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CheckinConversionDTO> list = statisticsService.buildCheckinRateByRoom(startDate, endDate);
        return ResultView.success(list);
    }

    /**
     * 学院 / 班级维度统计。
     */
    @GetMapping("/dept-class-statistics")
    public ResultView<List<DeptClassStatisticsDTO>> deptClassStatistics(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DeptClassStatisticsDTO> list = statisticsService.buildDeptClassStatistics(startDate, endDate);
        return ResultView.success(list);
    }

    /**
     * 座位使用分布。
     */
    @GetMapping("/seat-usage")
    public ResultView<List<SeatUsageDTO>> seatUsage(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "topN", required = false) Integer topN) {
        List<SeatUsageDTO> list = statisticsService.buildSeatUsage(startDate, endDate, topN);
        return ResultView.success(list);
    }

    /**
     * 提前预约时长分布。
     */
    @GetMapping("/advance-reservation-lead-time")
    public ResultView<List<TrendPointDTO>> advanceReservationLeadTime(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<TrendPointDTO> list = statisticsService.buildAdvanceReservationLeadTimeDistribution(startDate, endDate);
        return ResultView.success(list);
    }
}
