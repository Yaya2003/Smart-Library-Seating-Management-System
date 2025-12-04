package com.example.statistics.service;

import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.TrendPointDTO;

import java.time.LocalDate;
import java.util.List;

public interface StatisticsService {

    OverviewStatisticsDTO buildOverviewStatistics();

    List<TrendPointDTO> buildReservationTrend(LocalDate date);

    List<TrendPointDTO> buildTimeSlotDistribution(LocalDate date);
}

