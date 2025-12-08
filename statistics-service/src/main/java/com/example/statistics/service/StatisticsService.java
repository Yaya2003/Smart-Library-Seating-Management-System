package com.example.statistics.service;

import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.TrendPointDTO;
import com.example.statistics.dto.RoomUsageRankDTO;
import com.example.statistics.dto.RoomTimeSlotHeatDTO;
import com.example.statistics.dto.ViolationCancelTrendDTO;
import com.example.statistics.dto.CheckinConversionDTO;
import com.example.statistics.dto.DeptClassStatisticsDTO;
import com.example.statistics.dto.SeatUsageDTO;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    OverviewStatisticsDTO buildOverviewStatistics();

    List<TrendPointDTO> buildReservationTrend(LocalDate date);

    List<TrendPointDTO> buildTimeSlotDistribution(LocalDate date);

    /**
     * 阅览室利用率排行（按时间范围聚合）。
     */
    List<RoomUsageRankDTO> buildRoomUsageRank(LocalDate startDate, LocalDate endDate);

    /**
     * 阅览室 x 时间段 热力图数据。
     */
    List<RoomTimeSlotHeatDTO> buildRoomTimeSlotHeat(LocalDate startDate, LocalDate endDate);

    /**
     * 违约 / 取消趋势（近 N 天）。
     */
    List<ViolationCancelTrendDTO> buildViolationCancelTrend(int days);

    /**
     * 按时间段统计签到转化率。
     */
    List<CheckinConversionDTO> buildCheckinRateByTimeSlot(LocalDate startDate, LocalDate endDate);

    /**
     * 按阅览室统计签到转化率。
     */
    List<CheckinConversionDTO> buildCheckinRateByRoom(LocalDate startDate, LocalDate endDate);

    /**
     * 学院 / 班级维度统计。
     */
    List<DeptClassStatisticsDTO> buildDeptClassStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 座位使用分布。
     */
    List<SeatUsageDTO> buildSeatUsage(LocalDate startDate, LocalDate endDate, Integer topN);

    /**
     * 提前预约时长分布。
     */
    List<TrendPointDTO> buildAdvanceReservationLeadTimeDistribution(LocalDate startDate, LocalDate endDate);
}
