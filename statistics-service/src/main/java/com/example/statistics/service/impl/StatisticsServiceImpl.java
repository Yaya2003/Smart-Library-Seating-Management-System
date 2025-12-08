package com.example.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.domain.enums.ReservationStatus;
import com.example.domain.po.Reservation;
import com.example.domain.po.Room;
import com.example.domain.po.Seat;
import com.example.domain.po.User;
import com.example.statistics.dto.CheckinConversionDTO;
import com.example.statistics.dto.DeptClassStatisticsDTO;
import com.example.statistics.dto.OverviewStatisticsDTO;
import com.example.statistics.dto.RoomTimeSlotHeatDTO;
import com.example.statistics.dto.RoomUsageRankDTO;
import com.example.statistics.dto.SeatUsageDTO;
import com.example.statistics.dto.TrendPointDTO;
import com.example.statistics.dto.ViolationCancelTrendDTO;
import com.example.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final com.example.mapper.ReservationMapper reservationMapper;
    private final com.example.mapper.UserMapper userMapper;
    private final com.example.mapper.SeatMapper seatMapper;
    private final com.example.mapper.RoomMapper roomMapper;

    @Override
    public OverviewStatisticsDTO buildOverviewStatistics() {
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        // 今日预约总数
        long todayReservationCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getDate, todayStr)
        );

        // 今日已签到
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

    @Override
    public List<RoomUsageRankDTO> buildRoomUsageRank(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Reservation>> byRoom = reservations.stream()
                .filter(r -> r.getRoomId() != null)
                .collect(Collectors.groupingBy(Reservation::getRoomId));

        List<Seat> seats = seatMapper.selectList(null);
        Map<Long, Long> roomSeatCountMap = seats.stream()
                .filter(s -> s.getRoomId() != null && (s.getStatus() == null || Objects.equals(s.getStatus(), 1)))
                .collect(Collectors.groupingBy(Seat::getRoomId, Collectors.counting()));

        List<RoomUsageRankDTO> list = new ArrayList<>();
        for (Map.Entry<Long, List<Reservation>> entry : byRoom.entrySet()) {
            Long roomId = entry.getKey();
            List<Reservation> roomReservations = entry.getValue();

            long reservationCount = roomReservations.stream()
                    .filter(r -> !ReservationStatus.CANCELLED.name().equals(r.getStatus()))
                    .count();
            long checkedInCount = roomReservations.stream()
                    .filter(r -> ReservationStatus.CHECKED_IN.name().equals(r.getStatus()))
                    .count();

            long seatCount = roomSeatCountMap.getOrDefault(roomId, 0L);
            double occupancyRate = seatCount == 0 ? 0d : (double) checkedInCount / seatCount;

            RoomUsageRankDTO dto = new RoomUsageRankDTO();
            dto.setRoomId(roomId);
            dto.setRoomName(roomReservations.get(0).getRoomName());
            dto.setReservationCount(reservationCount);
            dto.setCheckedInCount(checkedInCount);
            dto.setSeatCount(seatCount);
            dto.setOccupancyRate(occupancyRate);
            list.add(dto);
        }

        list.sort(Comparator.comparingDouble(RoomUsageRankDTO::getOccupancyRate).reversed());
        return list;
    }

    @Override
    public List<RoomTimeSlotHeatDTO> buildRoomTimeSlotHeat(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        List<Seat> seats = seatMapper.selectList(null);
        Map<Long, Long> roomSeatCountMap = seats.stream()
                .filter(s -> s.getRoomId() != null && (s.getStatus() == null || Objects.equals(s.getStatus(), 1)))
                .collect(Collectors.groupingBy(Seat::getRoomId, Collectors.counting()));

        Map<String, RoomTimeSlotHeatDTO> map = new HashMap<>();
        for (Reservation r : reservations) {
            Long roomId = r.getRoomId();
            if (roomId == null || r.getTimeSlot() == null) {
                continue;
            }
            if (ReservationStatus.CANCELLED.name().equals(r.getStatus())) {
                continue;
            }
            String key = roomId + "#" + r.getTimeSlot();
            RoomTimeSlotHeatDTO dto = map.computeIfAbsent(key, k -> {
                RoomTimeSlotHeatDTO item = new RoomTimeSlotHeatDTO();
                item.setRoomId(roomId);
                item.setRoomName(r.getRoomName());
                item.setTimeSlot(r.getTimeSlot());
                return item;
            });
            dto.setReservationCount(dto.getReservationCount() + 1);
            if (ReservationStatus.CHECKED_IN.name().equals(r.getStatus())) {
                dto.setCheckedInCount(dto.getCheckedInCount() + 1);
            }
        }

        for (RoomTimeSlotHeatDTO dto : map.values()) {
            long seatCount = roomSeatCountMap.getOrDefault(dto.getRoomId(), 0L);
            double rate = seatCount == 0 ? 0d : (double) dto.getCheckedInCount() / seatCount;
            dto.setUtilizationRate(rate);
        }

        return new ArrayList<>(map.values());
    }

    @Override
    public List<ViolationCancelTrendDTO> buildViolationCancelTrend(int days) {
        int safeDays = days <= 0 ? 7 : Math.min(days, 90);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(safeDays - 1L);

        String startStr = start.toString();
        String endStr = end.toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, startStr)
                        .le(Reservation::getDate, endStr)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<String, List<Reservation>> byDate = reservations.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.groupingBy(Reservation::getDate));

        List<ViolationCancelTrendDTO> result = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String dateStr = cursor.toString();
            List<Reservation> list = byDate.getOrDefault(dateStr, List.of());

            long breachCount = list.stream()
                    .filter(r -> ReservationStatus.BREACH.name().equals(r.getStatus()))
                    .count();
            long cancelCount = list.stream()
                    .filter(r -> ReservationStatus.CANCELLED.name().equals(r.getStatus()))
                    .count();
            long effectiveCount = list.stream()
                    .filter(r -> !ReservationStatus.CANCELLED.name().equals(r.getStatus()))
                    .count();

            ViolationCancelTrendDTO dto = new ViolationCancelTrendDTO();
            dto.setDate(dateStr);
            dto.setBreachCount(breachCount);
            dto.setCancelCount(cancelCount);
            dto.setViolationRate(effectiveCount == 0 ? 0d : (double) breachCount / effectiveCount);
            result.add(dto);

            cursor = cursor.plusDays(1);
        }

        return result;
    }

    @Override
    public List<CheckinConversionDTO> buildCheckinRateByTimeSlot(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<String, List<Reservation>> bySlot = reservations.stream()
                .filter(r -> r.getTimeSlot() != null)
                .collect(Collectors.groupingBy(Reservation::getTimeSlot));

        List<CheckinConversionDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Reservation>> entry : bySlot.entrySet()) {
            String slot = entry.getKey();
            List<Reservation> list = entry.getValue();

            long reservationCount = list.stream()
                    .filter(r -> !ReservationStatus.CANCELLED.name().equals(r.getStatus()))
                    .count();
            long checkedInCount = list.stream()
                    .filter(r -> ReservationStatus.CHECKED_IN.name().equals(r.getStatus()))
                    .count();

            CheckinConversionDTO dto = new CheckinConversionDTO();
            dto.setLabel(timeSlotLabel(slot));
            dto.setReservationCount(reservationCount);
            dto.setCheckedInCount(checkedInCount);
            dto.setCheckinRate(reservationCount == 0 ? 0d : (double) checkedInCount / reservationCount);
            result.add(dto);
        }

        return result;
    }

    @Override
    public List<CheckinConversionDTO> buildCheckinRateByRoom(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Reservation>> byRoom = reservations.stream()
                .filter(r -> r.getRoomId() != null)
                .collect(Collectors.groupingBy(Reservation::getRoomId));

        List<CheckinConversionDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Reservation>> entry : byRoom.entrySet()) {
            Long roomId = entry.getKey();
            List<Reservation> list = entry.getValue();

            long reservationCount = list.stream()
                    .filter(r -> !ReservationStatus.CANCELLED.name().equals(r.getStatus()))
                    .count();
            long checkedInCount = list.stream()
                    .filter(r -> ReservationStatus.CHECKED_IN.name().equals(r.getStatus()))
                    .count();

            CheckinConversionDTO dto = new CheckinConversionDTO();
            dto.setLabel(list.get(0).getRoomName());
            dto.setReservationCount(reservationCount);
            dto.setCheckedInCount(checkedInCount);
            dto.setCheckinRate(reservationCount == 0 ? 0d : (double) checkedInCount / reservationCount);
            result.add(dto);
        }

        result.sort(Comparator.comparingLong(CheckinConversionDTO::getReservationCount).reversed());
        return result;
    }

    @Override
    public List<DeptClassStatisticsDTO> buildDeptClassStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = reservations.stream()
                .map(Reservation::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return List.of();
        }

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        Map<String, DeptClassStatisticsDTO> statsMap = new HashMap<>();
        for (Reservation r : reservations) {
            if (ReservationStatus.CANCELLED.name().equals(r.getStatus())) {
                continue;
            }
            User user = userMap.get(r.getUserId());
            if (user == null) {
                continue;
            }
            String department = user.getDepartment();
            String className = user.getClassName();
            String key = (department == null ? "" : department) + "#" + (className == null ? "" : className);

            DeptClassStatisticsDTO dto = statsMap.computeIfAbsent(key, k -> {
                DeptClassStatisticsDTO item = new DeptClassStatisticsDTO();
                item.setDepartment(department);
                item.setClassName(className);
                return item;
            });

            dto.setReservationCount(dto.getReservationCount() + 1);
            if (ReservationStatus.BREACH.name().equals(r.getStatus())) {
                dto.setBreachCount(dto.getBreachCount() + 1);
            }
        }

        for (DeptClassStatisticsDTO dto : statsMap.values()) {
            dto.setViolationRate(dto.getReservationCount() == 0 ? 0d :
                    (double) dto.getBreachCount() / dto.getReservationCount());
        }

        List<DeptClassStatisticsDTO> result = new ArrayList<>(statsMap.values());
        result.sort(Comparator.comparingLong(DeptClassStatisticsDTO::getReservationCount).reversed());
        return result;
    }

    @Override
    public List<SeatUsageDTO> buildSeatUsage(LocalDate startDate, LocalDate endDate, Integer topN) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> seatCountMap = reservations.stream()
                .map(Reservation::getSeatId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        Set<Long> seatIds = seatCountMap.keySet();
        if (seatIds.isEmpty()) {
            return List.of();
        }

        List<Seat> seats = seatMapper.selectBatchIds(seatIds);
        Map<Long, Seat> seatMap = seats.stream()
                .collect(Collectors.toMap(Seat::getId, s -> s, (a, b) -> a));

        Set<Long> roomIds = seats.stream()
                .map(Seat::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Room> rooms = roomMapper.selectBatchIds(roomIds);
        Map<Long, Room> roomMap = rooms.stream()
                .collect(Collectors.toMap(Room::getRoomId, r -> r, (a, b) -> a));

        List<SeatUsageDTO> list = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : seatCountMap.entrySet()) {
            Long seatId = entry.getKey();
            Seat seat = seatMap.get(seatId);
            if (seat == null) {
                continue;
            }
            Room room = roomMap.get(seat.getRoomId());

            SeatUsageDTO dto = new SeatUsageDTO();
            dto.setSeatId(seatId);
            dto.setSeatName(seat.getName());
            if (room != null) {
                dto.setRoomId(room.getRoomId());
                dto.setRoomName(room.getRoomName());
            }
            dto.setReservationCount(entry.getValue());
            list.add(dto);
        }

        list.sort(Comparator.comparingLong(SeatUsageDTO::getReservationCount).reversed());

        if (topN != null && topN > 0 && list.size() > topN) {
            return list.subList(0, topN);
        }
        return list;
    }

    @Override
    public List<TrendPointDTO> buildAdvanceReservationLeadTimeDistribution(LocalDate startDate, LocalDate endDate) {
        LocalDate[] range = resolveDateRange(startDate, endDate);
        String start = range[0].toString();
        String end = range[1].toString();

        List<Reservation> reservations = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .ge(Reservation::getDate, start)
                        .le(Reservation::getDate, end)
        );
        if (reservations.isEmpty()) {
            return List.of();
        }

        Map<String, Long> bucketCount = new HashMap<>();
        for (Reservation r : reservations) {
            if (r.getCreateAt() == null || r.getDate() == null || r.getTimeSlot() == null) {
                continue;
            }
            LocalDateTime createdAt = toLocalDateTime(r.getCreateAt());
            LocalDateTime startAt = buildStartDateTime(r.getDate(), r.getTimeSlot());
            if (createdAt == null || startAt == null) {
                continue;
            }
            long minutes = java.time.Duration.between(createdAt, startAt).toMinutes();
            if (minutes < 0) {
                minutes = 0;
            }
            double hours = minutes / 60.0;

            String label;
            if (hours < 1) {
                label = "<1小时";
            } else if (hours < 3) {
                label = "1-3小时";
            } else if (hours < 24) {
                label = "3-24小时";
            } else {
                label = ">1天";
            }

            bucketCount.merge(label, 1L, Long::sum);
        }

        List<String> labels = List.of("<1小时", "1-3小时", "3-24小时", ">1天");
        List<TrendPointDTO> result = new ArrayList<>();
        for (String label : labels) {
            long value = bucketCount.getOrDefault(label, 0L);
            result.add(new TrendPointDTO(label, value));
        }
        return result;
    }

    private LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate;
        LocalDate end = endDate;
        if (start == null && end == null) {
            start = LocalDate.now();
            end = start;
        } else if (start == null) {
            start = end;
        } else if (end == null) {
            end = start;
        }
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return new LocalDate[]{start, end};
    }

    private String timeSlotLabel(String slot) {
        if ("MORNING".equalsIgnoreCase(slot)) {
            return "上午";
        }
        if ("AFTERNOON".equalsIgnoreCase(slot)) {
            return "下午";
        }
        if ("EVENING".equalsIgnoreCase(slot)) {
            return "晚上";
        }
        return slot;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    private LocalDateTime buildStartDateTime(String dateStr, String timeSlot) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time;
            if ("MORNING".equalsIgnoreCase(timeSlot)) {
                time = LocalTime.of(8, 0);
            } else if ("AFTERNOON".equalsIgnoreCase(timeSlot)) {
                time = LocalTime.of(12, 30);
            } else if ("EVENING".equalsIgnoreCase(timeSlot)) {
                time = LocalTime.of(18, 0);
            } else {
                time = LocalTime.of(8, 0);
            }
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            return null;
        }
    }
}
